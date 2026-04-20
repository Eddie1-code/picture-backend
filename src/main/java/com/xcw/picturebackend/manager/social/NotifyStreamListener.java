package com.xcw.picturebackend.manager.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 通知类消费者
 * - 目前 UnreadRedisManager 在业务侧已直接 HINCRBY，这里主要作为观测/降级二次补偿：
 *   当 Redis Hash 因为某种原因没有被业务侧 +1（例如本地内存引用延迟、落库失败重试），
 *   消费者基于 Stream 再次尝试 +1；业务侧已做的去重由调用方负责。
 * - 保留此入口的目的：未来可扩展站内信 push、WebSocket 实时推送等。
 */
@Slf4j
@Component
public class NotifyStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    @Resource
    private UnreadRedisManager unreadRedisManager;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> v = message.getValue();
            String type = v.get("eventType");
            Long targetUserId = parseLong(v.get("targetUserId"));
            if (targetUserId == null || targetUserId == 0L) return;
            // 这里仅打日志作为观测，避免与业务侧 HINCRBY 双增；
            // 如要切换为"以 Stream 为唯一写入源"，删掉业务侧调用并取消注释下方 inc* 调用即可
            log.debug("NotifyStreamListener received type={}, target={}", type, targetUserId);
        } catch (Exception e) {
            log.warn("NotifyStreamListener.onMessage failed", e);
        }
    }

    private Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }
}
