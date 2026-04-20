package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.userfollow.FollowActionRequest;
import com.xcw.picturebackend.model.dto.userfollow.FollowListRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.UserFollowVO;
import com.xcw.picturebackend.service.UserFollowService;
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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/userFollow")
public class UserFollowController {

    @Resource
    private UserFollowService userFollowService;

    @Resource
    private UserService userService;

    /**
     * 关注 / 取关
     */
    @PostMapping("/toggle")
    public BaseResponse<Boolean> toggle(@RequestBody FollowActionRequest request,
                                        HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean followed = userFollowService.toggleFollow(request, loginUser);
        return ResultUtils.success(followed);
    }

    /**
     * 查询关注/粉丝列表
     */
    @PostMapping("/list")
    public BaseResponse<IPage<UserFollowVO>> list(@RequestBody FollowListRequest request,
                                                  HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        IPage<UserFollowVO> page = userFollowService.listFollowOrFans(request, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 查询指定用户的关注数 / 粉丝数 + 当前登录用户是否已关注 TA
     */
    @GetMapping("/stat")
    public BaseResponse<Map<String, Object>> stat(@RequestParam("userId") Long userId,
                                                  HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);
        Map<String, Object> data = new HashMap<>();
        data.put("followCount", userFollowService.countFollowing(userId));
        data.put("fansCount", userFollowService.countFans(userId));
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(httpServletRequest);
        } catch (Exception ignored) {
        }
        if (loginUser != null && !loginUser.getId().equals(userId)) {
            boolean followed = userFollowService.isFollowing(loginUser.getId(), userId);
            boolean mutual = followed && userFollowService.isFollowing(userId, loginUser.getId());
            data.put("isFollowed", followed);
            data.put("isMutualFollow", mutual);
        } else {
            data.put("isFollowed", false);
            data.put("isMutualFollow", false);
        }
        return ResultUtils.success(data);
    }
}
