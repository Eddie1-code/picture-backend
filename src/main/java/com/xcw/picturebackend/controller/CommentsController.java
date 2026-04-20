package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.DeleteRequest;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.comment.CommentAddRequest;
import com.xcw.picturebackend.model.dto.comment.CommentQueryRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.CommentVO;
import com.xcw.picturebackend.service.CommentsService;
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
@RequestMapping("/comment")
public class CommentsController {

    @Resource
    private CommentsService commentsService;

    @Resource
    private UserService userService;

    /**
     * 发布评论 / 回复
     */
    @PostMapping("/add")
    public BaseResponse<CommentVO> addComment(@RequestBody CommentAddRequest request,
                                              HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        CommentVO vo = commentsService.addComment(request, loginUser);
        return ResultUtils.success(vo);
    }

    /**
     * 删除评论（作者本人 / 内容作者 / 管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteComment(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean ok = commentsService.deleteComment(deleteRequest.getId(), loginUser);
        return ResultUtils.success(ok);
    }

    /**
     * 拉某个目标的顶级评论分页（携带前 N 条子回复预览）
     */
    @PostMapping("/list/top")
    public BaseResponse<Page<CommentVO>> listTopLevel(@RequestBody CommentQueryRequest request,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(httpServletRequest);
        } catch (Exception ignore) {
            // 未登录也可以看评论
        }
        Page<CommentVO> page = commentsService.listTopLevelComments(request, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 展开某条顶级评论下的所有子回复
     */
    @GetMapping("/list/children")
    public BaseResponse<Page<CommentVO>> listChildren(@RequestParam Long rootCommentId,
                                                      @RequestParam(defaultValue = "1") long current,
                                                      @RequestParam(defaultValue = "10") long size,
                                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(rootCommentId == null || rootCommentId <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(httpServletRequest);
        } catch (Exception ignore) {
        }
        Page<CommentVO> page = commentsService.listChildComments(rootCommentId, current, size, loginUser);
        return ResultUtils.success(page);
    }
}
