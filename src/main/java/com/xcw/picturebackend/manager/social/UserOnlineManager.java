package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 用户在线状态：依赖客户端心跳刷新 TTL（默认 120s）
 * - 心跳：每 60s 前端发一次 /user/heartbeat（由 UserController 暴露）
 * - 判断在线：EXISTS / 批量 MGET
 * - 维护成本极低，无需 DB
 */
@Slf4j
@Component
public class UserOnlineManager {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void heartbeat(Long userId) {
        if (userId == null) return;
        try {
            stringRedisTemplate.opsForValue().set(
                    SocialRedisKey.userOnlineKey(userId),
                    String.valueOf(System.currentTimeMillis()),
                    SocialRedisKey.USER_ONLINE_TTL_SECONDS,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("UserOnlineManager.heartbeat failed, userId={}", userId, e);
        }
    }

    public boolean isOnline(Long userId) {
        if (userId == null) return false;
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(SocialRedisKey.userOnlineKey(userId)));
        } catch (Exception e) {
            log.warn("UserOnlineManager.isOnline failed, userId={}", userId, e);
            return false;
        }
    }

    public Set<Long> onlineSet(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Collections.emptySet();
        List<String> keys = new ArrayList<>(userIds.size());
        List<Long> ids = new ArrayList<>(userIds.size());
        for (Long id : userIds) {
            if (id == null) continue;
            keys.add(SocialRedisKey.userOnlineKey(id));
            ids.add(id);
        }
        if (keys.isEmpty()) return Collections.emptySet();
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values == null) return Collections.emptySet();
            Set<Long> result = new HashSet<>();
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i) != null) result.add(ids.get(i));
            }
            return result;
        } catch (Exception e) {
            log.warn("UserOnlineManager.onlineSet failed", e);
            return Collections.emptySet();
        }
    }
}
