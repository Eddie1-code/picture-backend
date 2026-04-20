package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.notify.NotifyListRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.NotifyItemVO;
import com.xcw.picturebackend.model.vo.NotifyUnreadVO;
import com.xcw.picturebackend.service.NotifyService;
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
@RequestMapping("/notify")
public class NotifyController {

    @Resource
    private NotifyService notifyService;

    @Resource
    private UserService userService;

    /**
     * 未读数聚合
     */
    @GetMapping("/unread")
    public BaseResponse<NotifyUnreadVO> unread(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(notifyService.getUnreadSummary(loginUser));
    }

    /**
     * 某类型消息分页
     */
    @PostMapping("/list")
    public BaseResponse<IPage<NotifyItemVO>> list(@RequestBody NotifyListRequest req,
                                                  HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(notifyService.listMessages(req, loginUser));
    }

    /**
     * 全部置已读（某类型）
     */
    @PostMapping("/readAll")
    public BaseResponse<Boolean> readAll(@RequestParam("notifyType") String notifyType,
                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(notifyService.markAllRead(notifyType, loginUser));
    }

    /**
     * 标记单条为已读
     */
    @PostMapping("/readOne")
    public BaseResponse<Boolean> readOne(@RequestParam("notifyType") String notifyType,
                                         @RequestParam("bizId") Long bizId,
                                         HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(notifyService.markOneRead(notifyType, bizId, loginUser));
    }
}
