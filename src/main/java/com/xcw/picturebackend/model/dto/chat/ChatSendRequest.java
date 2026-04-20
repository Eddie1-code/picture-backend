package com.xcw.picturebackend.model.dto.chat;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatSendRequest implements Serializable {

    /**
     * 收件人 userId
     */
    private Long receiverId;

    /**
     * 文本内容（text 必填；image 可选作为说明）
     */
    private String content;

    /**
     * text / image
     */
    private String messageType;

    /**
     * 图片 url（image 时必填）
     */
    private String messageUrl;

    private Long messageSize;

    /**
     * 幂等去重 ID
     */
    private String clientMsgId;

    /**
     * 回复的消息 ID
     */
    private Long replyId;

    /**
     * 关联图片 ID（分享图片时使用）
     */
    private Long pictureId;

    private static final long serialVersionUID = 1L;
}
