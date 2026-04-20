package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.InteractionLockManager;
import com.xcw.picturebackend.mapper.LikeRecordMapper;
import com.xcw.picturebackend.model.entity.LikeRecord;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.service.LikeRecordService;
import com.xcw.picturebackend.service.PictureService;
import com.xcw.picturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用点赞服务实现
 * - 同一用户对同一目标重复点击：2s Redis 锁拒绝 + DB 唯一键兜底
 * - 计数一致性：点赞 / 取消 与 targets 表 likeCount +/-1 在同一事务内
 */
@Slf4j
@Service
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord>
        implements LikeRecordService {

    @Resource
    private InteractionLockManager interactionLockManager;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long targetId, Integer targetType, User loginUser) {
        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR, "目标 id 非法");
        ThrowUtils.throwIf(!TargetTypeEnum.isValid(targetType), ErrorCode.PARAMS_ERROR, "目标类型非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = loginUser.getId();

        // 1) 短时重复点击保护
        if (!interactionLockManager.tryLockLike(userId, targetType, targetId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 2) 校验目标存在 + 找到目标所属用户 + 校验「允许点赞」
        Long targetUserId;
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            Picture p = pictureService.getById(targetId);
            ThrowUtils.throwIf(p == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (p.getAllowLike() != null && p.getAllowLike() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭点赞");
            }
            targetUserId = p.getUserId();
        } else if (TargetTypeEnum.SPACE.getValue() == targetType) {
            Space s = spaceService.getById(targetId);
            ThrowUtils.throwIf(s == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            targetUserId = s.getUserId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该目标类型");
        }

        // 3) 查询现状
        LikeRecord existed = this.lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType)
                .one();

        int newLiked;
        int delta;
        if (existed == null) {
            newLiked = 1;
            delta = +1;
        } else if (existed.getIsLiked() != null && existed.getIsLiked() == 1) {
            newLiked = 0;
            delta = -1;
        } else {
            newLiked = 1;
            delta = +1;
        }

        // 4) upsert 点赞记录
        baseMapper.upsertLike(userId, targetId, targetType, targetUserId, newLiked);

        // 5) 计数字段同事务更新（GREATEST 避免并发下为负）
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            pictureService.lambdaUpdate()
                    .eq(Picture::getId, targetId)
                    .setSql("likeCount = GREATEST(0, IFNULL(likeCount,0) + (" + delta + "))")
                    .update();
        }
        // SPACE 的 likeCount 暂未加字段，如需请按同样方式扩展。

        return newLiked == 1;
    }

    @Override
    public Set<Long> listLikedTargetIds(Long userId, Integer targetType, Collection<Long> targetIds) {
        if (userId == null || targetType == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptySet();
        }
        List<Long> list = baseMapper.listLikedTargetIds(userId, targetType, targetIds);
        return list == null ? Collections.emptySet() : new HashSet<>(list);
    }

    @Override
    public Map<Long, Long> countLikes(Integer targetType, Collection<Long> targetIds) {
        if (targetType == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = baseMapper.countLikes(targetType, targetIds);
        Map<Long, Long> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object tid = row.get("targetId");
            Object cnt = row.get("cnt");
            if (tid != null && cnt != null) {
                result.put(((Number) tid).longValue(), ((Number) cnt).longValue());
            }
        }
        return result;
    }
}
