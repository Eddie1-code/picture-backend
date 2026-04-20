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
 * 聊天消息明细
 *
 * @TableName chat_message
 */
@TableName(value = "chat_message")
@Data
public class ChatMessage implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long privateChatId;

    private Long senderId;

    private Long receiverId;

    private Long spaceId;

    private Long pictureId;

    private String content;

    /**
     * text / image
     */
    private String messageType;

    private String messageUrl;

    private Long messageSize;

    private String messageLocation;

    private Long replyId;

    /**
     * 0-未读 1-已读 2-已撤回
     */
    private Integer status;

    private String clientMsgId;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
