package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.entity.LikeRecord;
import com.xcw.picturebackend.model.entity.User;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 通用点赞服务
 */
public interface LikeRecordService extends IService<LikeRecord> {

    /**
     * 切换点赞状态：已赞则取消，否则点赞。返回切换后的最新状态：true=已赞, false=未赞。
     */
    boolean toggleLike(Long targetId, Integer targetType, User loginUser);

    /**
     * 批量查询当前用户已点赞的 targetId 集合（targetType 统一）
     */
    Set<Long> listLikedTargetIds(Long userId, Integer targetType, Collection<Long> targetIds);

    /**
     * 批量查询一批 target 的点赞数（带容错：targetIds 为空时返回空 map）
     */
    Map<Long, Long> countLikes(Integer targetType, Collection<Long> targetIds);
}
