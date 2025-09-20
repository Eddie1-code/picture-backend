package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.sharding.DynamicShardingManager;
import com.xcw.picturebackend.model.dto.space.SpaceAddRequest;
import com.xcw.picturebackend.model.dto.space.SpaceQueryRequest;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.mapper.SpaceMapper;
import com.xcw.picturebackend.model.entity.SpaceUser;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.SpaceLevelEnum;
import com.xcw.picturebackend.model.enums.SpaceRoleEnum;
import com.xcw.picturebackend.model.enums.SpaceTypeEnum;
import com.xcw.picturebackend.model.vo.SpaceVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.SpaceService;
import com.xcw.picturebackend.service.SpaceUserService;
import com.xcw.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author EddieXu
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-09-10 19:32:56
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private TransactionTemplate transactionTemplate;

    private final Map<Long, Object> lockMap = new ConcurrentHashMap<>();

    // 为了便于部署，注释掉动态分表功能
//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    @Override
    @Transactional
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充参数默认值
        // 转换实体类和 DTO
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isEmpty(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if(space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量和大小
        fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        this.validSpace(space, true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 4. 控制同一用户只能创建一个私有空间，以及一个团队空间
        // 这里使用了一个简单的锁机制，防止并发创建多个空间
        Object lock = lockMap.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery()
                            .eq(Space::getUserId, userId)
                            .eq(Space::getSpaceType, space.getSpaceType())
                            .exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个！");
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "保存空间到数据库失败");
                    // 如果是团队空间，关联新增团队成员记录
                    if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                        SpaceUser spaceUser = new SpaceUser();
                        spaceUser.setSpaceId(space.getId());
                        spaceUser.setUserId(userId);
                        spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        result = spaceUserService.save(spaceUser);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                    }
                    // 为了便于部署，注释掉动态分表功能
//                    // 动态创建图片分表【仅对团队空间分表】
//                    dynamicShardingManager.createSpacePictureTable(space);
                    // 返回新写入的数据 id
                    return space.getId();
                });
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                lockMap.remove(userId);
            }
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            if(spaceType == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }

        // 修改数据时，如果要改空间级别
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        // 先补上这段代码（一般空间类型不会变更！）
        if (spaceType != null && spaceTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        /*
         * 此处做了做了个小优化，不是针对每条数据都查询一次用户，而是先获取到要查询的用户 id 列表，即只发送一次查询用户表的的请求，再将查询到的值设置进图片对象中。
         */
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




