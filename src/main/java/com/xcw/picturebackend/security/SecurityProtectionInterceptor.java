package com.xcw.picturebackend.security;

import cn.hutool.core.util.StrUtil;
import com.xcw.picturebackend.config.SecurityProtectionProperties;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.manager.RateLimitManager;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityProtectionInterceptor implements HandlerInterceptor {

    private final SecurityProtectionProperties securityProtectionProperties;
    private final RequestIpResolver requestIpResolver;
    private final RequestSignatureService requestSignatureService;
    private final SecurityAlertService securityAlertService;
    private final RateLimitManager rateLimitManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = requestIpResolver.resolveClientIp(request);
        String userId = resolveUserId(request);
        checkBlacklist(path, clientIp, userId);
        checkCrawler(path, clientIp, userId, request.getHeader("User-Agent"));
        requestSignatureService.verifyIfRequired(request);
        applyGlobalRateLimit(path, method, userId, clientIp);
        return true;
    }

    private void checkBlacklist(String path, String clientIp, String userId) {
        if (!securityProtectionProperties.getBlacklist().isEnabled()) {
            return;
        }
        boolean blocked = isBlockedIp(clientIp) || isBlockedUser(userId);
        if (blocked) {
            log.warn("黑名单命中，拒绝访问 path={}, userId={}, ip={}", path, userId, clientIp);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "当前请求来源已被封禁");
        }
    }

    private boolean isBlockedIp(String ip) {
        String key = "security:blacklist:ip:" + StrUtil.blankToDefault(ip, "unknown");
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private boolean isBlockedUser(String userId) {
        if ("anonymous".equals(userId)) {
            return false;
        }
        String key = "security:blacklist:user:" + userId;
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    private void applyGlobalRateLimit(String path, String method, String userId, String clientIp) {
        SecurityProtectionProperties.RateLimit rateLimit = securityProtectionProperties.getRateLimit();
        if (!rateLimit.isEnabled() || !shouldProtectPath(path, rateLimit)) {
            return;
        }
        boolean writeRequest = isWriteMethod(method);
        SecurityProtectionProperties.Rule rule = selectRequestRule(path, method, writeRequest, rateLimit);
        boolean failOpenOnError = !writeRequest;
        String route = method + ":" + path;
        RateLimitManager.RateLimitDecision decision = rateLimitManager.checkRouteUserIpRateLimit(
                route,
                userId,
                clientIp,
                rule.getLimit(),
                rule.getWindowSeconds(),
                failOpenOnError
        );
        if (decision.isDegraded()) {
            log.warn("全局限流降级触发, method={}, path={}, userId={}, ip={}, failOpen={}",
                    method, path, userId, clientIp, failOpenOnError);
        }
        if (!decision.isAllowed()) {
            securityAlertService.record429Event(path, userId, clientIp);
            if (!rateLimit.isEnforce()) {
                log.warn("限流仅记录模式命中，未阻断请求 path={}, userId={}, ip={}, count={}, key={}",
                        path, userId, clientIp, decision.getCount(), decision.getKey());
                return;
            }
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求过于频繁，请稍后再试");
        }
    }

    private void checkCrawler(String path, String clientIp, String userId, String userAgent) {
        SecurityProtectionProperties.Crawler crawler = securityProtectionProperties.getCrawler();
        if (!crawler.isEnabled() || !shouldProtectPath(path, crawler.getProtectedPathPrefixes())) {
            return;
        }
        if (crawler.isBlockEmptyUserAgent() && StrUtil.isBlank(userAgent)) {
            securityAlertService.recordCrawlerBlocked(path, userId, clientIp, "empty-user-agent");
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求特征异常，访问已被拒绝");
        }
        if (StrUtil.isBlank(userAgent)) {
            return;
        }
        String normalized = userAgent.toLowerCase(Locale.ROOT);
        for (String keyword : crawler.getBlockedUserAgentKeywords()) {
            if (StrUtil.isNotBlank(keyword) && normalized.contains(keyword.trim().toLowerCase(Locale.ROOT))) {
                securityAlertService.recordCrawlerBlocked(path, userId, clientIp, "ua-keyword:" + keyword);
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "检测到异常访问来源");
            }
        }
    }

    private SecurityProtectionProperties.Rule selectRequestRule(String path,
                                                                String method,
                                                                boolean writeRequest,
                                                                SecurityProtectionProperties.RateLimit rateLimit) {
        SecurityProtectionProperties.Rule customRule = selectCustomRule(path, method, rateLimit);
        if (customRule != null) {
            return customRule;
        }
        if (writeRequest) {
            return selectWriteRule(path, rateLimit);
        }
        return rateLimit.getReadRule();
    }

    private SecurityProtectionProperties.Rule selectWriteRule(String path, SecurityProtectionProperties.RateLimit rateLimit) {
        if (isStrictPath(path, rateLimit.getStrictPathPrefixes())) {
            return rateLimit.getStrictRule();
        }
        return rateLimit.getWriteRule();
    }

    private SecurityProtectionProperties.Rule selectCustomRule(String path,
                                                               String method,
                                                               SecurityProtectionProperties.RateLimit rateLimit) {
        if (rateLimit.getCustomPathRules() == null || rateLimit.getCustomPathRules().isEmpty()) {
            return null;
        }
        for (SecurityProtectionProperties.PathRule rule : rateLimit.getCustomPathRules()) {
            if (rule == null || StrUtil.isBlank(rule.getPathPrefix())) {
                continue;
            }
            String prefix = rule.getPathPrefix().trim();
            if (!path.startsWith(prefix)) {
                continue;
            }
            if (!methodMatched(method, rule.getMethods())) {
                continue;
            }
            return new SecurityProtectionProperties.Rule(rule.getWindowSeconds(), rule.getLimit());
        }
        return null;
    }

    private boolean shouldProtectPath(String path, SecurityProtectionProperties.RateLimit rateLimit) {
        if (isStrictPath(path, rateLimit.getExcludePathPrefixes())) {
            return false;
        }
        List<String> includes = rateLimit.getIncludePathPrefixes();
        if (includes == null || includes.isEmpty()) {
            return true;
        }
        return isStrictPath(path, includes);
    }

    private boolean shouldProtectPath(String path, List<String> includePrefixes) {
        if (includePrefixes == null || includePrefixes.isEmpty()) {
            return true;
        }
        return isStrictPath(path, includePrefixes);
    }

    private boolean isStrictPath(String path, List<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (StrUtil.isNotBlank(prefix) && path.startsWith(prefix.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean methodMatched(String method, List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            return true;
        }
        for (String allowedMethod : methods) {
            if (StrUtil.isNotBlank(allowedMethod)
                    && method.equalsIgnoreCase(allowedMethod.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isWriteMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private String resolveUserId(HttpServletRequest request) {
        try {
            User user = userService.getLoginUser(request);
            if (user != null && user.getId() != null) {
                return String.valueOf(user.getId());
            }
        } catch (Exception ignored) {
            // 登录态获取失败时按匿名处理
        }
        return "anonymous";
    }
}
