package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户的隐私开关一键更新请求。
 * 任意字段为 null 表示「不修改」，0/1 表示「关闭/开启」。
 */
@Data
public class UserPrivacyUpdateRequest implements Serializable {

    /**
     * 是否允许被私聊：1-允许 0-禁止
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注：1-允许 0-禁止
     */
    private Integer allowFollow;

    /**
     * 是否公开关注列表：1-公开 0-隐藏
     */
    private Integer showFollowList;

    /**
     * 是否公开粉丝列表：1-公开 0-隐藏
     */
    private Integer showFansList;

    /**
     * 是否公开「喜欢」列表：1-公开 0-隐藏
     */
    private Integer showLikeList;

    /**
     * 是否公开「收藏」列表：1-公开 0-隐藏
     */
    private Integer showFavoriteList;

    private static final long serialVersionUID = 1L;
}
