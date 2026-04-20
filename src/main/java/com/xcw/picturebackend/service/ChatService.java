package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.PageRequest;
import com.xcw.picturebackend.model.dto.chat.ChatMessageListRequest;
import com.xcw.picturebackend.model.dto.chat.ChatSendRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.ChatConversationVO;
import com.xcw.picturebackend.model.vo.ChatMessageVO;

public interface ChatService {

    /**
     * 发送私信
     */
    ChatMessageVO sendMessage(ChatSendRequest request, User loginUser);

    /**
     * 列出会话
     */
    IPage<ChatConversationVO> listConversations(PageRequest request, User loginUser);

    /**
     * 某会话的历史消息
     */
    IPage<ChatMessageVO> listMessages(ChatMessageListRequest request, User loginUser);

    /**
     * 标记与对方的会话已读
     */
    boolean markConversationRead(Long targetUserId, User loginUser);

    /**
     * 获取未读总数
     */
    long getUnreadTotal(User loginUser);
}
