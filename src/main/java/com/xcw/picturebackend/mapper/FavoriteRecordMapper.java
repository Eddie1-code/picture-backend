package com.xcw.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcw.picturebackend.model.entity.FavoriteRecord;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 通用收藏表 Mapper
 */
public interface FavoriteRecordMapper extends BaseMapper<FavoriteRecord> {

    /**
     * 原子 upsert：切换收藏状态
     *
     * @param isFavorite 1-收藏 0-取消
     * @return 影响行数
     */
    int upsertFavorite(@Param("userId") Long userId,
                       @Param("targetId") Long targetId,
                       @Param("targetType") Integer targetType,
                       @Param("targetUserId") Long targetUserId,
                       @Param("isFavorite") Integer isFavorite);

    /**
     * 查询当前用户在一组 target 中已收藏的 targetId
     */
    List<Long> listFavoriteTargetIds(@Param("userId") Long userId,
                                     @Param("targetType") Integer targetType,
                                     @Param("targetIds") Collection<Long> targetIds);
}
