package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.userfollow.FollowActionRequest;
import com.xcw.picturebackend.model.dto.userfollow.FollowListRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.entity.UserFollow;
import com.xcw.picturebackend.model.vo.UserFollowVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserFollowService extends IService<UserFollow> {

    /**
     * 关注 / 取关
     */
    boolean toggleFollow(FollowActionRequest request, User loginUser);

    /**
     * 分页查询某个用户的「关注列表」或「粉丝列表」
     */
    IPage<UserFollowVO> listFollowOrFans(FollowListRequest request, User loginUser);

    /**
     * 当前登录用户是否关注了指定用户（单查）
     */
    boolean isFollowing(Long followerId, Long followingId);

    /**
     * 批量判断：followerId 是否关注了给定的每个 targetId
     */
    Map<Long, Boolean> batchIsFollowing(Long followerId, List<Long> targetIds);

    /**
     * 某用户的关注数 / 粉丝数
     */
    long countFollowing(Long userId);

    long countFans(Long userId);

    /**
     * 获取某用户的关注者 id 集合（已加入缓存）
     */
    Set<Long> getFollowingIds(Long userId);

    /**
     * 获取某用户的粉丝 id 集合（已加入缓存）
     */
    Set<Long> getFansIds(Long userId);
}
