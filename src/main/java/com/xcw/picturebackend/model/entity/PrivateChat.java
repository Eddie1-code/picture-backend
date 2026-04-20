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
 * 私聊会话表
 *
 * @TableName private_chat
 */
@TableName(value = "private_chat")
@Data
public class PrivateChat implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会话发起方（较小的一方）
     */
    private Long userId;

    /**
     * 会话目标方（较大的一方）
     */
    private Long targetUserId;

    private String lastMessage;

    private String lastMessageType;

    private Date lastMessageTime;

    private Integer userUnreadCount;

    private Integer targetUserUnreadCount;

    private String userChatName;

    private String targetUserChatName;

    /**
     * 0-陌生人  1-好友(双向关注)
     */
    private Integer chatType;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
