package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.interaction.LikeActionRequest;
import com.xcw.picturebackend.model.dto.interaction.LikeQueryRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.LikeVO;
import com.xcw.picturebackend.service.LikeRecordService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/like")
public class LikeRecordController {

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserService userService;

    /**
     * 切换点赞状态（已赞则取消，否则点赞）
     *
     * @return true=当前已点赞, false=当前未点赞
     */
    @PostMapping("/toggle")
    public BaseResponse<Boolean> toggleLike(@RequestBody LikeActionRequest request,
                                            HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean liked = likeRecordService.toggleLike(request.getTargetId(), request.getTargetType(), loginUser);
        return ResultUtils.success(liked);
    }

    /**
     * 「我 / 某用户」的点赞分页
     */
    @PostMapping("/my/list")
    public BaseResponse<Page<LikeVO>> listMyLikes(@RequestBody LikeQueryRequest request,
                                                  HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<LikeVO> page = likeRecordService.listMyLikes(request, loginUser);
        return ResultUtils.success(page);
    }
}
