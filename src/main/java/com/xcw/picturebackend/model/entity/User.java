package com.xcw.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

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
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    // ========== 社交互动字段 ==========

    /**
     * 是否允许被私聊：1-允许 0-禁止
     */
    private Integer allowPrivateChat;

    /**
     * 是否允许被关注：1-允许 0-禁止
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
     * 是否公开「喜欢」列表：1-展示 0-隐藏
     */
    private Integer showLikeList;

    /**
     * 是否公开「收藏」列表：1-展示 0-隐藏
     */
    private Integer showFavoriteList;

    /**
     * 个性签名
     */
    private String personalSign;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    @TableField(exist = false) // 标记该字段不参与数据库表的映射
    private static final long serialVersionUID = 1L; // 序列化版本号，用于类的序列化和反序列化
}