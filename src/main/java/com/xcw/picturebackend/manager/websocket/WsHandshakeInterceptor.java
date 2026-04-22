package com.xcw.picturebackend.manager.websocket;


import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.xcw.picturebackend.config.SecurityProtectionProperties;
import com.xcw.picturebackend.manager.RateLimitManager;
import com.xcw.picturebackend.manager.auth.StpKit;
import com.xcw.picturebackend.manager.auth.SpaceUserAuthManager;
import com.xcw.picturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xcw.picturebackend.security.RequestIpResolver;
import com.xcw.picturebackend.security.SecurityAlertService;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.SpaceTypeEnum;
import com.xcw.picturebackend.service.PictureService;
import com.xcw.picturebackend.service.SpaceService;
import com.xcw.picturebackend.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/22
 */

/**
 * WebSocket 握手拦截器
 * 用于在握手前进行用户认证和权限校验
 */
@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SecurityProtectionProperties securityProtectionProperties;

    @Resource
    private RateLimitManager rateLimitManager;

    @Resource
    private RequestIpResolver requestIpResolver;

    @Resource
    private SecurityAlertService securityAlertService;

    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 获取请求参数
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }

            // 先走常规鉴权（cookie/header）
            User loginUser = null;
            try {
                loginUser = userService.getLoginUser(servletRequest);
            } catch (Exception ignore) {
                // ignore，走 query token 兜底
            }
            // 若常规鉴权失败，则尝试从 query 中获取 token（适配前端 WebSocket）
            if (ObjUtil.isEmpty(loginUser)) {
                String tokenName = servletRequest.getParameter("tokenName");
                String tokenValue = servletRequest.getParameter("tokenValue");
                // 优先读取固定参数，其次尝试读取 tokenName 对应参数
                if (StrUtil.isBlank(tokenValue) && StrUtil.isNotBlank(tokenName)) {
                    tokenValue = servletRequest.getParameter(tokenName);
                }
                if (StrUtil.isBlank(tokenValue)) {
                    // 再兜底：读取当前 StpLogic 的 token-name 对应参数
                    tokenValue = servletRequest.getParameter(StpKit.SPACE.getTokenName());
                }
                if (StrUtil.isNotBlank(tokenValue)) {
                    try {
                        Object loginId = StpKit.SPACE.getLoginIdByToken(tokenValue);
                        if (ObjUtil.isNotEmpty(loginId)) {
                            Long userId = Long.valueOf(String.valueOf(loginId));
                            loginUser = userService.getById(userId);
                        }
                    } catch (Exception e) {
                        log.warn("WebSocket query token 校验失败", e);
                    }
                }
            }
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            if (!applyHandshakeRateLimit(servletRequest, String.valueOf(loginUser.getId()))) {
                log.warn("WebSocket 握手限流命中，拒绝握手");
                return false;
            }
            // 校验用户是否有该图片的权限
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("图片所属空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.info("图片所属空间不是团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            // 设置 attributes
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId)); // 记得转换为 Long 类型
        }
        return true;
    }

    private boolean applyHandshakeRateLimit(HttpServletRequest servletRequest, String userId) {
        SecurityProtectionProperties.WebSocket ws = securityProtectionProperties.getWebsocket();
        if (ws == null || !ws.isEnabled()) {
            return true;
        }
        SecurityProtectionProperties.Rule rule = ws.getHandshakeRule();
        if (rule == null) {
            return true;
        }
        String clientIp = requestIpResolver.resolveClientIp(servletRequest);
        RateLimitManager.RateLimitDecision decision = rateLimitManager.checkRouteUserIpRateLimit(
                "WS:/ws/picture/edit",
                userId,
                clientIp,
                rule.getLimit(),
                rule.getWindowSeconds(),
                false
        );
        if (decision.isDegraded()) {
            log.warn("WebSocket 握手限流降级, userId={}, ip={}", userId, clientIp);
        }
        if (!decision.isAllowed()) {
            securityAlertService.record429Event("/ws/picture/edit", userId, clientIp);
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, Exception exception) {
    }
}