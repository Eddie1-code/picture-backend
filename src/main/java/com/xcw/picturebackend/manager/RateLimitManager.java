package com.xcw.picturebackend.manager;

import cn.hutool.core.util.StrUtil;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import lombok.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 限流管理器
 */
@Slf4j
@Component
public class RateLimitManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 检查用户是否超过限流
     * @param userId 用户ID
     * @param keyPrefix 限流key前缀
     * @param limit 限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return 是否通过限流
     */
    public boolean checkRateLimit(Long userId, String keyPrefix, int limit, int windowSeconds) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        
        String key = keyPrefix + userId;
        try {
            // 获取当前时间戳
            long currentTime = System.currentTimeMillis();
            // 计算时间窗口的开始时间
            long windowStart = currentTime - windowSeconds * 1000L;
            
            // 使用Redis的ZSET结构实现滑动窗口限流
            // 1. 添加当前时间戳到ZSET
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
            // 2. 移除时间窗口外的记录
            stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            // 3. 设置ZSET的过期时间，避免内存泄漏
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            // 4. 获取时间窗口内的请求次数
            Long count = stringRedisTemplate.opsForZSet().zCard(key);
            
            log.info("用户 {} 在 {} 秒内的请求次数: {}", userId, windowSeconds, count);
            
            // 检查是否超过限制
            return count <= limit;
        } catch (Exception e) {
            log.error("限流检查失败", e);
            // 异常时默认通过，避免影响正常业务
            return true;
        }
    }

    /**
     * 检查AI扩图任务限流
     * @param userId 用户ID
     * @return 是否通过限流
     */
    public boolean checkOutPaintingRateLimit(Long userId) {
        // 限制单用户每分钟最多3次任务
        return checkRateLimit(userId, "out_painting:rate_limit:", 3, 60);
    }

    /**
     * 通用限流入口（当前为滑动窗口实现，后续可平滑切到令牌桶）。
     * <p>
     * 调用方统一走该方法，避免业务层直接耦合具体限流算法。
     */
    public RateLimitDecision checkRouteUserIpRateLimit(String route,
                                                       String userId,
                                                       String clientIp,
                                                       int limit,
                                                       int windowSeconds,
                                                       boolean failOpenOnError) {
        String safeRoute = sanitize(route);
        String safeUserId = sanitize(userId);
        String safeIp = sanitize(clientIp);
        String key = String.format("global:rate_limit:%s:%s:%s", safeRoute, safeUserId, safeIp);
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - windowSeconds * 1000L;
        try {
            stringRedisTemplate.opsForZSet().add(key, String.valueOf(currentTime), currentTime);
            stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
            stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            Long count = stringRedisTemplate.opsForZSet().zCard(key);
            long safeCount = count == null ? 0L : count;
            boolean allowed = safeCount <= limit;
            return new RateLimitDecision(allowed, false, safeCount, key);
        } catch (Exception e) {
            log.error("全局限流检查失败, key={}", key, e);
            return new RateLimitDecision(failOpenOnError, true, -1L, key);
        }
    }

    private String sanitize(String value) {
        if (StrUtil.isBlank(value)) {
            return "unknown";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9:_./-]", "_");
    }

    @Value
    public static class RateLimitDecision {
        boolean allowed;
        boolean degraded;
        long count;
        String key;
    }
}