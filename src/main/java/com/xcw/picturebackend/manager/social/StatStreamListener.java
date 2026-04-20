package com.xcw.picturebackend.manager.social;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 数据统计消费者：按日累计各类事件数量
 * 使用 Hash：picture:social:stat:day:{yyyyMMdd}  field -> count
 * 7 日自动过期，主要用于仪表盘 / 观测，不参与业务决策
 */
@Slf4j
@Component
public class StatStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    private static final String KEY_PREFIX = "picture:social:stat:day:";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> v = message.getValue();
            String type = v.get("eventType");
            if (type == null) return;
            String day = LocalDate.now().format(FMT);
            String key = KEY_PREFIX + day;
            stringRedisTemplate.opsForHash().increment(key, type, 1L);
            stringRedisTemplate.expire(key, 7L, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("StatStreamListener.onMessage failed", e);
        }
    }
}
