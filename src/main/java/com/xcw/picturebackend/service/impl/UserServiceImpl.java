package com.xcw.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.constant.UserConstant;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.auth.StpKit;
import com.xcw.picturebackend.model.dto.user.UserPrivacyUpdateRequest;
import com.xcw.picturebackend.model.dto.user.UserQueryRequest;
import com.xcw.picturebackend.model.dto.user.UserRegisterRequest;
import com.xcw.picturebackend.model.dto.user.UserUpdateRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.dto.user.VipCode;
import com.xcw.picturebackend.model.enums.UserRoleEnum;
import com.xcw.picturebackend.model.vo.LoginUserVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.UserService;
import com.xcw.picturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * @author 20339
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-15 19:11:18
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public long userRegister(UserRegisterRequest req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR, "参数为空");

        // 0. 验证码校验
        String captchaId = req.getCaptchaId();
        String captchaCode = req.getCaptchaCode();
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(captchaCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "请先完成验证码");
        }
        String redisKey = "captcha:" + captchaId;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (StrUtil.isBlank(cachedCode) || !cachedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR, "验证码错误或已过期");
        }
        stringRedisTemplate.delete(redisKey);

        String userAccount = req.getUserAccount();
        String userPassword = req.getUserPassword();
        String checkPassword = req.getCheckPassword();
        String userProfile = req.getUserProfile();
        String userAvatar = req.getUserAvatar();

        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        userAccount = StrUtil.trim(userAccount);
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于4");
        }

        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        // 展示昵称：未传或为空时与账号一致（账号即昵称）
        String userName = StrUtil.trim(req.getUserName());
        if (StrUtil.isBlank(userName)) {
            userName = userAccount;
        }
        if (userName.length() < 2 || userName.length() > 256) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称为 2-256 个字符");
        }
        if (StrUtil.isNotBlank(userProfile) && userProfile.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简介过长");
        }

        //2.检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        //3.密码一定要加盐加密
        String encryptedPassword = getEncryptPassword(userPassword);

        //4.将用户信息存入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName(userName);
        if (StrUtil.isNotBlank(userProfile)) {
            user.setUserProfile(StrUtil.trim(userProfile));
        }
        if (StrUtil.isNotBlank(userAvatar)) {
            user.setUserAvatar(userAvatar.trim());
        }
        user.setUserRole(UserRoleEnum.USER.getValue()); //默认用户角色
        boolean saveResult = this.save(user);
        /**
         * if(!saveResult) {
         *      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
         * }
         *
         */
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "用户注册失败");

        return user.getId(); //返回新用户的ID
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HttpServletRequest 对象
     * @return 登录成功的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        //改进写法：
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号错误");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码错误");

        //2.查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null || !BCrypt.checkpw(userPassword, user.getUserPassword())) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        //4. 记录用户登录态到 Sa-Token（会话数据存 Redis）
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user); //返回登录用户信息对象
    }


    /**
     * 加密用户密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        return BCrypt.hashpw(userPassword, BCrypt.gensalt());
    }

    /**
     * 获取脱敏后的登录用户的视图对象
     *
     * @param user 用户实体
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null; //如果用户不存在，返回null
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        //使用BeanUtil.copyProperties进行属性拷贝
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 已登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 基于 Sa-Token 获取当前 token 对应的登录用户（支持 Header Token，天然适配多标签页）
        long userId = StpKit.SPACE.getLoginIdAsLong();
        User currentUser = this.getById(userId);
        if (currentUser == null) {
            log.info("getLoginUser failed, userId cannot match user");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或已注销");
        }
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request HttpServletRequest 对象
     * @return 是否注销成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 仅注销当前 token（不影响同账号其他标签页或其他设备会话）
        if (!StpKit.SPACE.isLogin()) {
            log.info("user logout failed, user not logged in");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录或已注销");
        }
        StpKit.SPACE.logout();
        return true; //返回注销成功
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获得脱敏后的用户列表
     *
     * @param userList 用户实体
     * @return 脱敏后的用户视图对象列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
//        int current = userQueryRequest.getCurrent();
//        int pageSize = userQueryRequest.getPageSize();
//        String sortField = userQueryRequest.getSortField();
//        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //添加查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        return queryWrapper;
    }


    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    // region ------- 以下代码为用户兑换会员功能 --------

    // 新增依赖注入
    @Autowired
    private ResourceLoader resourceLoader;
    // 文件读写锁（确保并发安全）
    private final ReentrantLock fileLock = new ReentrantLock();
    // VIP 角色常量（根据你的需求自定义）
    private static final String VIP_ROLE = "vip";

    /**
     * 兑换会员
     *
     * @param user    当前用户
     * @param vipCode 会员兑换码
     * @return 是否兑换成功
     */
    @Override
    public boolean exchangeVip(User user, String vipCode) {
        // 1. 参数校验
        if (user == null || StrUtil.isBlank(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 读取并校验兑换码
        VipCode targetCode = validateAndMarkVipCode(vipCode);
        // 3. 更新用户信息
        updateUserVipInfo(user, targetCode.getCode());
        return true;
    }

    /**
     * 校验兑换码并标记为已使用
     */
    private VipCode validateAndMarkVipCode(String vipCode) {
        fileLock.lock(); // 加锁保证文件操作原子性
        try {
            // 读取 JSON 文件
            JSONArray jsonArray = readVipCodeFile();
            // 查找匹配的未使用兑换码
            List<VipCode> codes = JSONUtil.toList(jsonArray, VipCode.class);
            VipCode target = codes.stream()
                    .filter(code -> code.getCode().equals(vipCode) && !code.isHasUsed())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "无效的兑换码"));
            // 标记为已使用
            target.setHasUsed(true);
            // 写回文件
            writeVipCodeFile(JSONUtil.parseArray(codes));
            return target;
        } finally {
            fileLock.unlock();
        }
    }

    /**
     * 读取兑换码文件
     */
    private JSONArray readVipCodeFile() {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            String content = FileUtil.readString(resource.getFile(), StandardCharsets.UTF_8);
            return JSONUtil.parseArray(content);
        } catch (IOException e) {
            log.error("读取兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 写入兑换码文件
     */
    private void writeVipCodeFile(JSONArray jsonArray) {
        try {
            Resource resource = resourceLoader.getResource("classpath:biz/vipCode.json");
            FileUtil.writeString(jsonArray.toStringPretty(), resource.getFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("更新兑换码文件失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙");
        }
    }

    /**
     * 更新用户会员信息
     */
    private void updateUserVipInfo(User user, String usedVipCode) {
        // 计算过期时间（当前时间 + 1 年）
        Date expireTime = DateUtil.offsetMonth(new Date(), 12); // 计算当前时间加 1 年后的时间
        // 构建更新对象
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setVipExpireTime(expireTime); // 设置过期时间
        updateUser.setVipCode(usedVipCode);     // 记录使用的兑换码
        updateUser.setUserRole(VIP_ROLE);       // 修改用户角色
        // 执行更新
        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "开通会员失败，操作数据库失败");
        }
    }
    // endregion ------- 以下代码为用户兑换会员功能 --------


    /**
     * 用户个人中心修改用户个人信息
     *
     * @param userUpdateRequest 用户更新请求对象
     * @param request           HttpServletRequest 对象
     * @return 是否更新成功
     */
    @Override
    public UserVO updateMyProfile(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新请求不能为空");
        }
        //1.获取当前登录用户
        User currentUser = this.getLoginUser(request);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或已注销");
        }
        //2.校验更新请求参数
        Long id = userUpdateRequest.getId();
        if (id == null || !id.equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不匹配");
        }
        String userName = userUpdateRequest.getUserName();
        String userPassword = userUpdateRequest.getUserPassword();
        String userAvatar = userUpdateRequest.getUserAvatar();
        String userProfile = userUpdateRequest.getUserProfile();

        //3.更新用户信息
        User userToUpdate = new User();
        userToUpdate.setId(id);
        userToUpdate.setUserName(userName);
        // 如果用户提供了新密码，则校验并更新；若未提供或为空，则保留原密码（从数据库读取）
        if (StrUtil.isNotBlank(userPassword)) {
            // 校验密码长度
            ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码长度小于8");
            userToUpdate.setUserPassword(getEncryptPassword(userPassword));
        } else {
            // 保留原密码：为了保险起见，明确设置为当前数据库中的密码（避免因框架配置问题将其置为 null）
            User existing = this.getById(id);
            if (existing != null && existing.getUserPassword() != null) {
                userToUpdate.setUserPassword(existing.getUserPassword());
            }
        }
        userToUpdate.setUserAvatar(userAvatar);
        userToUpdate.setUserProfile(userProfile);
        boolean updateResult = this.updateById(userToUpdate);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "用户信息更新失败");
        //4.更新成功后，返回更新后的用户信息
        User updatedUser = this.getById(id);
        if (updatedUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新后的用户信息不存在");
        }
        //返回脱敏后的用户信息
        return this.getUserVO(updatedUser);
    }

    // ========== 隐私开关 ==========

    @Override
    public UserVO getMyPrivacy(HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 重新读一次，保证拿到最新 flags（登录态缓存的可能是登录瞬间的快照）
        User fresh = this.getById(loginUser.getId());
        return this.getUserVO(fresh != null ? fresh : loginUser);
    }

    @Override
    public UserVO updateMyPrivacy(UserPrivacyUpdateRequest req, HttpServletRequest servletRequest) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = this.getLoginUser(servletRequest);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        User toUpdate = new User();
        toUpdate.setId(loginUser.getId());
        boolean hasChange = false;
        if (req.getAllowPrivateChat() != null) {
            toUpdate.setAllowPrivateChat(normalizeBit(req.getAllowPrivateChat()));
            hasChange = true;
        }
        if (req.getAllowFollow() != null) {
            toUpdate.setAllowFollow(normalizeBit(req.getAllowFollow()));
            hasChange = true;
        }
        if (req.getShowFollowList() != null) {
            toUpdate.setShowFollowList(normalizeBit(req.getShowFollowList()));
            hasChange = true;
        }
        if (req.getShowFansList() != null) {
            toUpdate.setShowFansList(normalizeBit(req.getShowFansList()));
            hasChange = true;
        }
        if (req.getShowLikeList() != null) {
            toUpdate.setShowLikeList(normalizeBit(req.getShowLikeList()));
            hasChange = true;
        }
        if (req.getShowFavoriteList() != null) {
            toUpdate.setShowFavoriteList(normalizeBit(req.getShowFavoriteList()));
            hasChange = true;
        }
        if (hasChange) {
            boolean ok = this.updateById(toUpdate);
            ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "隐私设置保存失败");
        }
        User fresh = this.getById(loginUser.getId());
        return this.getUserVO(fresh != null ? fresh : loginUser);
    }

    private static Integer normalizeBit(Integer v) {
        if (v == null) return null;
        return v != 0 ? 1 : 0;
    }
}









