package com.xcw.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcw.picturebackend.model.entity.UserFollow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户关注关系 Mapper
 */
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * upsert：插入或更新 follow 记录，实现关注/取关切换
     */
    int upsertFollow(@Param("followerId") Long followerId,
                     @Param("followingId") Long followingId,
                     @Param("followStatus") Integer followStatus);

    /**
     * 查询 userId 关注的人的 id 列表（关注中）
     */
    List<Long> listFollowingIds(@Param("followerId") Long followerId);

    /**
     * 查询关注 userId 的人的 id 列表（粉丝）
     */
    List<Long> listFollowerIds(@Param("followingId") Long followingId);

    /**
     * 更新双向关注标记
     */
    int updateMutualFlag(@Param("userAId") Long userAId,
                         @Param("userBId") Long userBId,
                         @Param("isMutual") Integer isMutual);
}
