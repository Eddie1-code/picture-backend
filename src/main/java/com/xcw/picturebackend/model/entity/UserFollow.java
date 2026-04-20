package com.xcw.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户关注关系
 *
 * @TableName userfollows
 */
@TableName(value = "userfollows")
@Data
public class UserFollow implements Serializable {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long followId;

    /**
     * 关注者（主动关注方）用户 ID
     */
    private Long followerId;

    /**
     * 被关注者用户 ID
     */
    private Long followingId;

    /**
     * 关注状态：1-关注中 0-已取消
     */
    private Integer followStatus;

    /**
     * 是否双向关注：1-是 0-否
     */
    private Integer isMutual;

    /**
     * 被关注方是否已读该关注
     */
    private Integer isRead;

    /**
     * 最后交互时间
     */
    private Date lastInteractionTime;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
