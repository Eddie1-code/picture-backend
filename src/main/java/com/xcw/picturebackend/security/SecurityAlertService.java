package com.xcw.picturebackend.security;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.xcw.picturebackend.config.SecurityProtectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityAlertService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SecurityProtectionProperties securityProtectionProperties;

    public void record429Event(String path, String userId, String ip) {
        SecurityProtectionProperties.Alert alert = securityProtectionProperties.getAlert();
        if (!alert.isEnabled()) {
            return;
        }
        String minute = DateUtil.format(DateUtil.date(), "yyyyMMddHHmm");
        long globalCount = increment("security:alert:429:global:" + minute, 120);
        if (globalCount >= alert.getGlobal429PerMinuteThreshold()) {
            emitOnce("security:alert:notify:global:" + minute, "429 命中激增",
                    "path=" + path + ", currentMinuteCount=" + globalCount);
        }
        String safeIp = StrUtil.blankToDefault(ip, "unknown");
        long ipCount = increment("security:alert:429:ip:" + safeIp + ":" + minute, 120);
        if (ipCount >= alert.getSingleIp429PerMinuteThreshold()) {
            emitOnce("security:alert:notify:ip:" + safeIp + ":" + minute, "单 IP 爆发",
                    "ip=" + safeIp + ", path=" + path + ", currentMinuteCount=" + ipCount);
        }
        if ("anonymous".equals(userId)) {
            long anonymousCount = increment("security:alert:429:anonymous:" + minute, 120);
            if (anonymousCount >= alert.getAnonymous429PerMinuteThreshold()) {
                emitOnce("security:alert:notify:anonymous:" + minute, "匿名流量突增",
                        "path=" + path + ", currentMinuteCount=" + anonymousCount);
            }
        }
    }

    public void recordCrawlerBlocked(String path, String userId, String ip, String reason) {
        SecurityProtectionProperties.Alert alert = securityProtectionProperties.getAlert();
        if (!alert.isEnabled()) {
            return;
        }
        String minute = DateUtil.format(DateUtil.date(), "yyyyMMddHHmm");
        String safeIp = StrUtil.blankToDefault(ip, "unknown");
        String safeUser = StrUtil.blankToDefault(userId, "anonymous");
        String reasonValue = StrUtil.blankToDefault(reason, "unknown");
        long count = increment("security:alert:crawler:block:" + safeIp + ":" + minute, 120);
        emitOnce("security:alert:notify:crawler:" + safeIp + ":" + minute,
                "疑似爬虫已拦截",
                "ip=" + safeIp + ", userId=" + safeUser + ", path=" + path + ", reason=" + reasonValue + ", currentMinuteCount=" + count);
    }

    private long increment(String key, int ttlSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
        return count == null ? 0L : count;
    }

    private void emitOnce(String dedupeKey, String title, String content) {
        Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(dedupeKey, "1", 60, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(first)) {
            log.warn("SECURITY_ALERT | {} | {}", title, content);
        }
    }
}
