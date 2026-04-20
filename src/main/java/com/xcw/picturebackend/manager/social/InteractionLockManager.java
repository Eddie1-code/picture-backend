package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 互动防重复点击锁：点赞 / 收藏 / 发评论 等高频幂等动作。
 * 失败不抛异常，靠数据库唯一键兜底；用 Redis 只是快速拒绝 2s 内的重复点击。
 */
@Slf4j
@Component
public class InteractionLockManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取一把短 TTL 锁，拿不到返回 false。
     */
    public boolean tryLock(String key) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(key, "1", SocialRedisKey.LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("InteractionLock acquire failed, fallback to pass. key={}", key, e);
            // Redis 异常时降级放行，由 DB 唯一键和业务幂等兜底
            return true;
        }
    }

    public boolean tryLockLike(Long userId, Integer targetType, Long targetId) {
        return tryLock(SocialRedisKey.likeLockKey(userId, targetType, targetId));
    }

    public boolean tryLockFavorite(Long userId, Integer targetType, Long targetId) {
        return tryLock(SocialRedisKey.favoriteLockKey(userId, targetType, targetId));
    }

    public boolean tryLockComment(Long userId) {
        return tryLock(SocialRedisKey.commentLockKey(userId));
    }

    public boolean tryLockFollow(Long followerId, Long followingId) {
        return tryLock(SocialRedisKey.followLockKey(followerId, followingId));
    }
}
