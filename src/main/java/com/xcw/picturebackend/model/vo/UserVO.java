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

    private static final long serialVersionUID = 1L;
}

