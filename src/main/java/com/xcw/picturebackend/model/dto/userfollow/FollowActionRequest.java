package com.xcw.picturebackend.model.dto.userfollow;

import lombok.Data;

import java.io.Serializable;

/**
 * 关注/取关 请求
 */
@Data
public class FollowActionRequest implements Serializable {

    /**
     * 被关注者用户 ID
     */
    private Long targetUserId;

    /**
     * 是否关注：true-关注 false-取关
     */
    private Boolean follow;

    private static final long serialVersionUID = 1L;
}
