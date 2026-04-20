package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 关注/粉丝列表项 VO（扁平化用户 + 关注关系状态）
 */
@Data
public class UserFollowVO implements Serializable {

    /**
     * 目标用户 ID（关注的人 / 粉丝的 ID）
     */
    private Long userId;

    private String userName;

    private String userAvatar;

    private String userProfile;

    private String personalSign;

    private String userRole;

    /**
     * 该关注关系创建时间
     */
    private Date followTime;

    /**
     * 当前登录用户是否已关注 TA
     */
    private Boolean isFollowed;

    /**
     * 是否互相关注
     */
    private Boolean isMutual;

    private static final long serialVersionUID = 1L;
}
