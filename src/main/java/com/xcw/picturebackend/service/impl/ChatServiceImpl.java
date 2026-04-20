package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.PageRequest;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.RedisLockUtil;
import com.xcw.picturebackend.manager.social.UnreadRedisManager;
import com.xcw.picturebackend.mapper.ChatMessageMapper;
import com.xcw.picturebackend.mapper.PrivateChatMapper;
import com.xcw.picturebackend.model.dto.chat.ChatMessageListRequest;
import com.xcw.picturebackend.model.dto.chat.ChatSendRequest;
import com.xcw.picturebackend.model.entity.ChatMessage;
import com.xcw.picturebackend.model.entity.PrivateChat;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.ChatConversationVO;
import com.xcw.picturebackend.model.vo.ChatMessageVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.ChatService;
import com.xcw.picturebackend.service.UserFollowService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 私信会话服务
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MESSAGE_PAGE_SIZE_MAX = 50;
    private static final int MESSAGE_CONTENT_MAX = 2000;
    private static final int PREVIEW_MAX = 200;

    @Resource
    private PrivateChatMapper privateChatMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private UserService userService;

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private UnreadRedisManager unreadRedisManager;

    @Resource
    private RedisLockUtil redisLockUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO sendMessage(ChatSendRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long senderId = loginUser.getId();
        Long receiverId = request.getReceiverId();
        ThrowUtils.throwIf(receiverId == null || receiverId <= 0, ErrorCode.PARAMS_ERROR, "接收人非法");
        ThrowUtils.throwIf(senderId.equals(receiverId), ErrorCode.PARAMS_ERROR, "不能给自己发消息");

        String type = StrUtil.blankToDefault(request.getMessageType(), "text");
        if (!"text".equals(type) && !"image".equals(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的消息类型");
        }
        if ("text".equals(type)) {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getContent()), ErrorCode.PARAMS_ERROR, "内容不能为空");
            ThrowUtils.throwIf(request.getContent().length() > MESSAGE_CONTENT_MAX,
                    ErrorCode.PARAMS_ERROR, "内容过长");
        } else {
            ThrowUtils.throwIf(StrUtil.isBlank(request.getMessageUrl()), ErrorCode.PARAMS_ERROR, "图片地址不能为空");
        }

        User receiver = userService.getById(receiverId);
        ThrowUtils.throwIf(receiver == null, ErrorCode.NOT_FOUND_ERROR, "对方用户不存在");
        if (receiver.getAllowPrivateChat() != null && receiver.getAllowPrivateChat() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "对方未开放私信");
        }

        // 幂等：先走 Redis 锁；重复请求快速识别并回取已有消息
        if (StrUtil.isNotBlank(request.getClientMsgId())) {
            String key = SocialRedisKey.chatMsgLockKey(senderId, request.getClientMsgId());
            boolean first = redisLockUtil.tryIdempotent(key, 5L, java.util.concurrent.TimeUnit.MINUTES);
            if (!first) {
                ChatMessage existed = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getClientMsgId, request.getClientMsgId())
                        .eq(ChatMessage::getSenderId, senderId)
                        .last("LIMIT 1"));
                if (existed != null) {
                    return toMessageVO(existed, loginUser);
                }
                // 锁已被占但 DB 尚未落库：认为是竞态，拒绝掉这次以避免双写
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复发送");
            }
            // 首次通过后仍然兜底查一次（非必需，但兼容 Redis 短暂不可用或 TTL 漂移）
            ChatMessage existed = chatMessageMapper.selectOne(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getClientMsgId, request.getClientMsgId())
                    .eq(ChatMessage::getSenderId, senderId)
                    .last("LIMIT 1"));
            if (existed != null) {
                return toMessageVO(existed, loginUser);
            }
        }

        PrivateChat chat = findOrCreateConversation(senderId, receiverId);
        boolean isMutual = userFollowService.isFollowing(senderId, receiverId)
                && userFollowService.isFollowing(receiverId, senderId);

        ChatMessage msg = new ChatMessage();
        msg.setPrivateChatId(chat.getId());
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setContent(StrUtil.blankToDefault(request.getContent(), ""));
        msg.setMessageType(type);
        msg.setMessageUrl(request.getMessageUrl());
        msg.setMessageSize(request.getMessageSize());
        msg.setPictureId(request.getPictureId());
        msg.setReplyId(request.getReplyId());
        msg.setStatus(0);
        msg.setClientMsgId(request.getClientMsgId());
        chatMessageMapper.insert(msg);

        // 更新会话：lastMessage / 对方未读 +1 / 会话类型
        String preview = "image".equals(type) ? "[图片]" :
                (msg.getContent().length() > PREVIEW_MAX ? msg.getContent().substring(0, PREVIEW_MAX) : msg.getContent());
        PrivateChat update = new PrivateChat();
        update.setId(chat.getId());
        update.setLastMessage(preview);
        update.setLastMessageType(type);
        update.setLastMessageTime(new Date());
        update.setChatType(isMutual ? 1 : 0);
        privateChatMapper.updateById(update);

        // 未读计数（谁收消息，谁加未读）
        boolean senderIsUserA = chat.getUserId().equals(senderId);
        if (senderIsUserA) {
            privateChatMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateChat>()
                    .eq(PrivateChat::getId, chat.getId())
                    .setSql("targetUserUnreadCount = IFNULL(targetUserUnreadCount,0) + 1"));
        } else {
            privateChatMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateChat>()
                    .eq(PrivateChat::getId, chat.getId())
                    .setSql("userUnreadCount = IFNULL(userUnreadCount,0) + 1"));
        }
        // Redis 未读计数：给接收方私信项 +1
        unreadRedisManager.incChat(receiverId);

        // 会话列表 ZSET：双方各自维护一份，score=时间戳，member=对方 userId
        try {
            long ts = System.currentTimeMillis();
            org.springframework.data.redis.core.StringRedisTemplate t = stringRedisTemplate();
            if (t != null) {
                t.opsForZSet().add(SocialRedisKey.chatConvListKey(senderId), String.valueOf(receiverId), ts);
                t.opsForZSet().add(SocialRedisKey.chatConvListKey(receiverId), String.valueOf(senderId), ts);
            }
        } catch (Exception ignored) {
        }
        return toMessageVO(msg, loginUser);
    }

    /**
     * 避免为了 ZSET 操作单独注入 StringRedisTemplate — 通过 UnreadRedisManager 暴露的同源模板访问。
     * 此处以 Spring 容器提供的 Bean 为准。
     */
    @Resource
    private org.springframework.data.redis.core.StringRedisTemplate injectedRedisTemplate;

    private org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
        return injectedRedisTemplate;
    }

    private PrivateChat findOrCreateConversation(Long uidA, Long uidB) {
        long small = Math.min(uidA, uidB);
        long big = Math.max(uidA, uidB);
        PrivateChat chat = privateChatMapper.selectOne(new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getUserId, small)
                .eq(PrivateChat::getTargetUserId, big)
                .last("LIMIT 1"));
        if (chat != null) {
            if (Integer.valueOf(1).equals(chat.getIsDelete())) {
                PrivateChat revive = new PrivateChat();
                revive.setId(chat.getId());
                revive.setIsDelete(0);
                privateChatMapper.updateById(revive);
            }
            return chat;
        }
        chat = new PrivateChat();
        chat.setUserId(small);
        chat.setTargetUserId(big);
        chat.setChatType(0);
        chat.setUserUnreadCount(0);
        chat.setTargetUserUnreadCount(0);
        privateChatMapper.insert(chat);
        return chat;
    }

    @Override
    public IPage<ChatConversationVO> listConversations(PageRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        long current = Math.max(1, request == null ? 1 : request.getCurrent());
        long size = Math.min(30, Math.max(1, request == null ? 20 : request.getPageSize()));
        Long uid = loginUser.getId();

        Page<PrivateChat> page = new Page<>(current, size);
        LambdaQueryWrapper<PrivateChat> qw = new LambdaQueryWrapper<PrivateChat>()
                .and(w -> w.eq(PrivateChat::getUserId, uid).or().eq(PrivateChat::getTargetUserId, uid))
                .eq(PrivateChat::getIsDelete, 0)
                .orderByDesc(PrivateChat::getLastMessageTime);
        privateChatMapper.selectPage(page, qw);

        Page<ChatConversationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<PrivateChat> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> otherIds = records.stream().map(c -> otherId(uid, c)).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = userService.listByIds(otherIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        List<ChatConversationVO> voList = records.stream().map(c -> {
            ChatConversationVO vo = new ChatConversationVO();
            vo.setId(c.getId());
            Long other = otherId(uid, c);
            vo.setTargetUser(userMap.get(other));
            vo.setLastMessage(c.getLastMessage());
            vo.setLastMessageType(c.getLastMessageType());
            vo.setLastMessageTime(c.getLastMessageTime());
            vo.setChatType(c.getChatType());
            if (uid.equals(c.getUserId())) {
                vo.setUnreadCount(c.getUserUnreadCount() == null ? 0 : c.getUserUnreadCount());
                vo.setRemarkName(c.getUserChatName());
            } else {
                vo.setUnreadCount(c.getTargetUserUnreadCount() == null ? 0 : c.getTargetUserUnreadCount());
                vo.setRemarkName(c.getTargetUserChatName());
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private Long otherId(Long uid, PrivateChat c) {
        return uid.equals(c.getUserId()) ? c.getTargetUserId() : c.getUserId();
    }

    @Override
    public IPage<ChatMessageVO> listMessages(ChatMessageListRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null || request.getTargetUserId() == null, ErrorCode.PARAMS_ERROR);
        Long uid = loginUser.getId();
        Long other = request.getTargetUserId();
        ThrowUtils.throwIf(uid.equals(other), ErrorCode.PARAMS_ERROR);
        long small = Math.min(uid, other);
        long big = Math.max(uid, other);

        PrivateChat chat = privateChatMapper.selectOne(new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getUserId, small)
                .eq(PrivateChat::getTargetUserId, big)
                .last("LIMIT 1"));
        long size = Math.min(MESSAGE_PAGE_SIZE_MAX, Math.max(1, request.getPageSize()));
        Page<ChatMessage> page = new Page<>(Math.max(1, request.getCurrent()), size);
        if (chat == null) {
            Page<ChatMessageVO> voPage = new Page<>(1, size, 0);
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        LambdaQueryWrapper<ChatMessage> qw = new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getPrivateChatId, chat.getId())
                .eq(ChatMessage::getIsDelete, 0)
                .orderByDesc(ChatMessage::getId);
        if (request.getBeforeId() != null && request.getBeforeId() > 0) {
            qw.lt(ChatMessage::getId, request.getBeforeId());
        }
        chatMessageMapper.selectPage(page, qw);

        Page<ChatMessageVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<ChatMessage> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Set<Long> senderIds = records.stream().map(ChatMessage::getSenderId).collect(Collectors.toSet());
        Map<Long, UserVO> userMap = userService.listByIds(senderIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        List<ChatMessageVO> voList = records.stream().map(m -> {
            ChatMessageVO v = new ChatMessageVO();
            BeanUtils.copyProperties(m, v);
            v.setSender(userMap.get(m.getSenderId()));
            return v;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    private ChatMessageVO toMessageVO(ChatMessage m, User loginUser) {
        ChatMessageVO v = new ChatMessageVO();
        BeanUtils.copyProperties(m, v);
        if (loginUser != null && loginUser.getId().equals(m.getSenderId())) {
            v.setSender(userService.getUserVO(loginUser));
        } else {
            User u = userService.getById(m.getSenderId());
            if (u != null) v.setSender(userService.getUserVO(u));
        }
        return v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markConversationRead(Long targetUserId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(targetUserId == null, ErrorCode.PARAMS_ERROR);
        Long uid = loginUser.getId();
        long small = Math.min(uid, targetUserId);
        long big = Math.max(uid, targetUserId);
        PrivateChat chat = privateChatMapper.selectOne(new LambdaQueryWrapper<PrivateChat>()
                .eq(PrivateChat::getUserId, small)
                .eq(PrivateChat::getTargetUserId, big)
                .last("LIMIT 1"));
        if (chat == null) return true;

        PrivateChat update = new PrivateChat();
        update.setId(chat.getId());
        if (uid.equals(chat.getUserId())) {
            update.setUserUnreadCount(0);
        } else {
            update.setTargetUserUnreadCount(0);
        }
        privateChatMapper.updateById(update);

        // 同步消息状态
        ChatMessage tpl = new ChatMessage();
        tpl.setStatus(1);
        chatMessageMapper.update(tpl, new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getPrivateChatId, chat.getId())
                .eq(ChatMessage::getReceiverId, uid)
                .eq(ChatMessage::getStatus, 0));
        // 整体会话都已读时：失效整体未读 Hash；下次 getUnreadTotal 会从 DB 重建准确值
        unreadRedisManager.invalidate(uid);
        return true;
    }

    @Override
    public long getUnreadTotal(User loginUser) {
        if (loginUser == null) return 0L;
        Long uid = loginUser.getId();
        List<PrivateChat> list = privateChatMapper.selectList(new LambdaQueryWrapper<PrivateChat>()
                .and(w -> w.eq(PrivateChat::getUserId, uid).or().eq(PrivateChat::getTargetUserId, uid))
                .eq(PrivateChat::getIsDelete, 0));
        long total = 0L;
        for (PrivateChat c : list) {
            if (uid.equals(c.getUserId())) {
                total += c.getUserUnreadCount() == null ? 0 : c.getUserUnreadCount();
            } else {
                total += c.getTargetUserUnreadCount() == null ? 0 : c.getTargetUserUnreadCount();
            }
        }
        return total;
    }
}
