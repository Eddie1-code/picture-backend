package com.xcw.picturebackend.manager.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 轻量分布式锁：SET NX PX + 带 token 的 Lua 解锁。
 * 用于幂等去重场景，不做自动续期；TTL 由调用方按业务评估设置。
 */
@Slf4j
@Component
public class RedisLockUtil {

    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(UNLOCK_LUA);
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试加锁（不阻塞）。成功返回 token 字符串，失败返回 null。
     */
    public String tryLock(String key, long ttl, TimeUnit unit) {
        if (key == null) return null;
        String token = UUID.randomUUID().toString();
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, token, ttl, unit);
            return Boolean.TRUE.equals(ok) ? token : null;
        } catch (Exception e) {
            log.warn("RedisLockUtil.tryLock failed, key={}, fallback=null(pass)", key, e);
            return null;
        }
    }

    /**
     * 仅持有锁者才可释放（Lua 保证原子）
     */
    public boolean unlock(String key, String token) {
        if (key == null || token == null) return false;
        try {
            Long r = stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
            return r != null && r > 0;
        } catch (Exception e) {
            log.warn("RedisLockUtil.unlock failed, key={}", key, e);
            return false;
        }
    }

    /**
     * 纯幂等去重：返回 true 表示首次；false 表示重复请求（已存在或设置失败）。
     * 不需要主动释放，自动过期。
     */
    public boolean tryIdempotent(String key, long ttl, TimeUnit unit) {
        if (key == null) return true;
        try {
            Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl, unit);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            log.warn("RedisLockUtil.tryIdempotent failed, key={}, pass", key, e);
            return true;
        }
    }
}
