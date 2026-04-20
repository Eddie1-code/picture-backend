package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.DeleteRequest;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.post.PostAddRequest;
import com.xcw.picturebackend.model.dto.post.PostEditRequest;
import com.xcw.picturebackend.model.dto.post.PostQueryRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.PostVO;
import com.xcw.picturebackend.service.PostService;
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

/**
 * 帖子 / 动态相关接口
 */
@Slf4j
@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private PostService postService;

    @Resource
    private UserService userService;

    /**
     * 发帖
     */
    @PostMapping("/add")
    public BaseResponse<Long> add(@RequestBody PostAddRequest request,
                                  HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long id = postService.addPost(request, loginUser);
        return ResultUtils.success(id);
    }

    /**
     * 编辑帖子（作者 / 管理员）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> edit(@RequestBody PostEditRequest request,
                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(postService.editPost(request, loginUser));
    }

    /**
     * 删除帖子（作者 / 管理员）
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest request,
                                        HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        return ResultUtils.success(postService.deletePost(request.getId(), loginUser));
    }

    /**
     * 帖子详情（自动增加浏览量）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PostVO> getVO(@RequestParam("id") Long id,
                                      HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PostVO vo = postService.getPostVO(id, httpServletRequest);
        // 后续 Redis Stream 上线后，浏览量改为异步累加；当前同步一下即可
        postService.increaseViewCount(id);
        return ResultUtils.success(vo);
    }

    /**
     * 分页列表：全局 feed / 某作者的帖子
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PostVO>> listByPage(@RequestBody PostQueryRequest request,
                                                 HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<PostVO> page = postService.listPostVOByPage(request, httpServletRequest);
        return ResultUtils.success(page);
    }

    /**
     * 我的帖子（可含仅自己可见）
     */
    @PostMapping("/my/list")
    public BaseResponse<Page<PostVO>> listMy(@RequestBody PostQueryRequest request,
                                             HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        request.setUserId(loginUser.getId());
        Page<PostVO> page = postService.listPostVOByPage(request, httpServletRequest);
        return ResultUtils.success(page);
    }
}
