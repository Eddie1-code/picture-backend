package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 会员过期时间
     */
    private Date vipExpireTime;

    /**
     * 会员兑换码
     */
    private String vipCode;

    /**
     * 会员编号
     */
    private String vipNumber;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    // ========== 社交字段 ==========

    /**
     * 个性签名
     */
    private String personalSign;

    /**
     * 关注数（我关注的人数）
     */
    private Long followCount;

    /**
     * 粉丝数（关注我的人数）
     */
    private Long fansCount;

    /**
     * 当前登录用户是否已关注 TA
     */
    private Boolean isFollowed;

    /**
     * 当前登录用户与 TA 是否互相关注
     */
    private Boolean isMutualFollow;

    // ========== 隐私开关（浏览者视角） ==========
    // 当查看者不是本人时，这些位用于前端决定 Tab 是否可见；
    // 当查看者为本人时，如实反映用户设置，用于「隐私设置」界面回显。

    /**
     * 是否允许私聊
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注
     */
    private Integer allowFollow;

    /**
     * 是否公开关注列表
     */
    private Integer showFollowList;

    /**
     * 是否公开粉丝列表
     */
    private Integer showFansList;

    /**
     * 是否公开「喜欢」列表
     */
    private Integer showLikeList;

    /**
     * 是否公开「收藏」列表
     */
    private Integer showFavoriteList;

    private static final long serialVersionUID = 1L;
}

