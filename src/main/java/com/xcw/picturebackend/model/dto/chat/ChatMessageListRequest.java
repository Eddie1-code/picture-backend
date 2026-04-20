package com.xcw.picturebackend.model.dto.chat;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatMessageListRequest extends PageRequest implements Serializable {

    /**
     * 会话对方 userId（根据登录者 + targetUserId 定位会话）
     */
    private Long targetUserId;

    /**
     * 只查这条 id 之前的（用于加载历史消息，按 id desc 翻页）
     */
    private Long beforeId;

    private static final long serialVersionUID = 1L;
}
