package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 未读计数 Redis 管理器
 * - 用 Hash 结构缓存用户各类未读计数（like / favorite / comment / follow / system / chat）
 * - 业务事件发生时通过 HINCRBY 增量累加；已读操作（包含整类置读）直接删除 Hash，下次读取时由 DB 重建
 * - 所有方法对 Redis 异常进行吞错降级，保证主流程不被缓存拖挂
 */
@Slf4j
@Component
public class UnreadRedisManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 原子自增某项未读计数；自动顺手刷新 TTL
     */
    public void increment(Long userId, String field, long delta) {
        if (userId == null || field == null || delta == 0) return;
        String key = SocialRedisKey.unreadHashKey(userId);
        try {
            stringRedisTemplate.opsForHash().increment(key, field, delta);
            stringRedisTemplate.expire(key, SocialRedisKey.UNREAD_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("UnreadRedisManager.increment failed. userId={}, field={}", userId, field, e);
        }
    }

    /**
     * 将某一项未读计数直接清零（HSET field 0），不重建其它字段
     */
    public void clearField(Long userId, String field) {
        if (userId == null || field == null) return;
        String key = SocialRedisKey.unreadHashKey(userId);
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.opsForHash().put(key, field, "0");
                stringRedisTemplate.expire(key, SocialRedisKey.UNREAD_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("UnreadRedisManager.clearField failed. userId={}, field={}", userId, field, e);
        }
    }

    /**
     * 整体失效：强制下一次聚合查询重建
     */
    public void invalidate(Long userId) {
        if (userId == null) return;
        try {
            stringRedisTemplate.delete(SocialRedisKey.unreadHashKey(userId));
        } catch (Exception e) {
            log.warn("UnreadRedisManager.invalidate failed. userId={}", userId, e);
        }
    }

    /**
     * 读取整 Hash；不存在或 Redis 异常返回 null。
     * 注意：返回 null 的语义是"缓存未命中"，调用方应回源重建。
     */
    public Map<String, Long> getAll(Long userId) {
        if (userId == null) return null;
        String key = SocialRedisKey.unreadHashKey(userId);
        try {
            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                return null;
            }
            Map<Object, Object> raw = stringRedisTemplate.opsForHash().entries(key);
            if (raw.isEmpty()) return null;
            Map<String, Long> result = new HashMap<>(raw.size());
            raw.forEach((k, v) -> {
                try {
                    result.put(String.valueOf(k), Long.parseLong(String.valueOf(v)));
                } catch (NumberFormatException ignore) {
                }
            });
            return result;
        } catch (Exception e) {
            log.warn("UnreadRedisManager.getAll failed. userId={}", userId, e);
            return null;
        }
    }

    /**
     * 全量写入（回源后调用），顺带设置 TTL
     */
    public void putAll(Long userId, Map<String, Long> counts) {
        if (userId == null || counts == null) return;
        String key = SocialRedisKey.unreadHashKey(userId);
        try {
            Map<String, String> asStr = new HashMap<>(counts.size());
            counts.forEach((k, v) -> asStr.put(k, String.valueOf(v == null ? 0 : v)));
            stringRedisTemplate.opsForHash().putAll(key, asStr);
            stringRedisTemplate.expire(key, SocialRedisKey.UNREAD_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("UnreadRedisManager.putAll failed. userId={}", userId, e);
        }
    }

    // ====== 业务侧便捷方法（语义化包装）======

    public void incLike(Long targetUserId) {
        increment(targetUserId, SocialRedisKey.UNREAD_FIELD_LIKE, 1);
    }

    public void incFavorite(Long targetUserId) {
        increment(targetUserId, SocialRedisKey.UNREAD_FIELD_FAVORITE, 1);
    }

    public void incComment(Long targetUserId) {
        increment(targetUserId, SocialRedisKey.UNREAD_FIELD_COMMENT, 1);
    }

    public void incFollow(Long followingUserId) {
        increment(followingUserId, SocialRedisKey.UNREAD_FIELD_FOLLOW, 1);
    }

    public void incChat(Long receiverUserId) {
        increment(receiverUserId, SocialRedisKey.UNREAD_FIELD_CHAT, 1);
    }
}
