package com.xcw.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcw.picturebackend.model.entity.LikeRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用点赞表 Mapper
 */
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    /**
     * 原子 upsert：不存在则插入(isLiked=1)，已存在则切换 isLiked 并刷新 lastLikeTime。
     *
     * @param userId       点赞者
     * @param targetId     目标 id
     * @param targetType   目标类型
     * @param targetUserId 目标作者
     * @param isLiked      1-点赞 0-取消
     * @return 影响行数（INSERT=1；ON DUPLICATE KEY UPDATE 真正改了=2；无变化=0）
     */
    int upsertLike(@Param("userId") Long userId,
                   @Param("targetId") Long targetId,
                   @Param("targetType") Integer targetType,
                   @Param("targetUserId") Long targetUserId,
                   @Param("isLiked") Integer isLiked);

    /**
     * 查询一组 targetId 中当前用户点赞的 targetId 列表（用于 VO 填充 isLiked）
     */
    List<Long> listLikedTargetIds(@Param("userId") Long userId,
                                  @Param("targetType") Integer targetType,
                                  @Param("targetIds") Collection<Long> targetIds);

    /**
     * 批量查询一组 targetId 的真实点赞数（从 like_record 聚合，isLiked=1）
     */
    List<Map<String, Object>> countLikes(@Param("targetType") Integer targetType,
                                         @Param("targetIds") Collection<Long> targetIds);
}
