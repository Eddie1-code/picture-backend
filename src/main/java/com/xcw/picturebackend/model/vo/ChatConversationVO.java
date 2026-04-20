package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 会话列表项 VO（登录用户视角）
 */
@Data
public class ChatConversationVO implements Serializable {

    private Long id;

    /**
     * 对方用户简要
     */
    private UserVO targetUser;

    private String lastMessage;

    private String lastMessageType;

    private Date lastMessageTime;

    /**
     * 登录用户在该会话的未读数
     */
    private Integer unreadCount;

    /**
     * 0-陌生人 1-好友
     */
    private Integer chatType;

    /**
     * 自己对对方的备注名
     */
    private String remarkName;

    private static final long serialVersionUID = 1L;
}
