package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ChatMessageVO implements Serializable {

    private Long id;

    private Long privateChatId;

    private Long senderId;

    private Long receiverId;

    private String content;

    private String messageType;

    private String messageUrl;

    private Long messageSize;

    private Long pictureId;

    private Long replyId;

    /**
     * 0-未读 1-已读 2-已撤回
     */
    private Integer status;

    private Date createTime;

    /**
     * 发送人简要
     */
    private UserVO sender;

    private static final long serialVersionUID = 1L;
}
