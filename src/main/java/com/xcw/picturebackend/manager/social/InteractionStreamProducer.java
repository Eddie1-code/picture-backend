package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 互动事件生产者 —— 以 Redis Stream 作为异步总线
 * 所有业务侧只依赖此组件；Redis 异常时吞错降级，不影响主流程
 */
@Slf4j
@Component
public class InteractionStreamProducer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public void publish(InteractionEvent event) {
        if (event == null || event.getEventType() == null) return;
        try {
            Map<String, String> body = new HashMap<>();
            body.put("eventType", event.getEventType());
            body.put("actorId", String.valueOf(nz(event.getActorId())));
            body.put("targetUserId", String.valueOf(nz(event.getTargetUserId())));
            body.put("targetType", String.valueOf(nz(event.getTargetType())));
            body.put("targetId", String.valueOf(nz(event.getTargetId())));
            body.put("ts", String.valueOf(event.getTs() == null ? System.currentTimeMillis() : event.getTs()));

            MapRecord<String, String, String> record = StreamRecords
                    .mapBacked(body)
                    .withStreamKey(SocialRedisKey.STREAM_INTERACTION);
            stringRedisTemplate.opsForStream().add(record);
        } catch (Exception e) {
            log.warn("InteractionStreamProducer.publish failed, event={}", event, e);
        }
    }

    private long nz(Number n) {
        return n == null ? 0L : n.longValue();
    }
}
