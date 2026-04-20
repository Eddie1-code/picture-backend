package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.PageRequest;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.chat.ChatMessageListRequest;
import com.xcw.picturebackend.model.dto.chat.ChatSendRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.ChatConversationVO;
import com.xcw.picturebackend.model.vo.ChatMessageVO;
import com.xcw.picturebackend.service.ChatService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private ChatService chatService;

    @Resource
    private UserService userService;

    /**
     * 发送一条私信
     */
    @PostMapping("/send")
    public BaseResponse<ChatMessageVO> send(@RequestBody ChatSendRequest request,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.sendMessage(request, loginUser));
    }

    /**
     * 会话列表
     */
    @PostMapping("/conversation/list")
    public BaseResponse<IPage<ChatConversationVO>> conversations(@RequestBody PageRequest req,
                                                                 HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.listConversations(req, loginUser));
    }

    /**
     * 消息历史
     */
    @PostMapping("/message/list")
    public BaseResponse<IPage<ChatMessageVO>> messages(@RequestBody ChatMessageListRequest req,
                                                       HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.listMessages(req, loginUser));
    }

    /**
     * 标记与对方会话已读
     */
    @PostMapping("/read")
    public BaseResponse<Boolean> markRead(@RequestParam("targetUserId") Long targetUserId,
                                          HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.markConversationRead(targetUserId, loginUser));
    }

    /**
     * 未读总数（用于头像旁红点）
     */
    @GetMapping("/unread")
    public BaseResponse<Long> unread(HttpServletRequest httpServletRequest) {
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(chatService.getUnreadTotal(loginUser));
    }
}
