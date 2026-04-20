package com.xcw.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.interaction.FavoriteActionRequest;
import com.xcw.picturebackend.model.dto.interaction.FavoriteQueryRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.FavoriteVO;
import com.xcw.picturebackend.service.FavoriteRecordService;
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
@RequestMapping("/favorite")
public class FavoriteController {

    @Resource
    private FavoriteRecordService favoriteRecordService;

    @Resource
    private UserService userService;

    /**
     * 切换收藏状态
     */
    @PostMapping("/toggle")
    public BaseResponse<Boolean> toggleFavorite(@RequestBody FavoriteActionRequest request,
                                                HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean favorite = favoriteRecordService.toggleFavorite(
                request.getTargetId(), request.getTargetType(), loginUser);
        return ResultUtils.success(favorite);
    }

    /**
     * 「我的收藏」分页
     */
    @PostMapping("/my/list")
    public BaseResponse<Page<FavoriteVO>> listMyFavorites(@RequestBody FavoriteQueryRequest request,
                                                          HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpServletRequest);
        Page<FavoriteVO> page = favoriteRecordService.listMyFavorites(request, loginUser);
        return ResultUtils.success(page);
    }
}
