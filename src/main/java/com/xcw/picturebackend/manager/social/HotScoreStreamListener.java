package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 热榜计算消费者
 * - 增量维护帖子热榜 ZSET：不同事件权重不同
 *   LIKE +3、FAVORITE +5、COMMENT +4、POST_VIEW +1、POST_PUBLISH +10 (新贴初始权重)
 * - UNLIKE / UNFAVORITE 扣回对应分，不低于 0 由定时重算兜底
 */
@Slf4j
@Component
public class HotScoreStreamListener implements StreamListener<String, MapRecord<String, String, String>> {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            Map<String, String> v = message.getValue();
            String type = v.get("eventType");
            Integer targetType = parseInt(v.get("targetType"));
            Long targetId = parseLong(v.get("targetId"));
            if (type == null || targetType == null || targetId == null || targetId == 0L) return;
            // 仅作用在 POST 上
            if (targetType != TargetTypeEnum.POST.getValue()) return;

            double weight = weightOf(type);
            if (weight == 0) return;
            stringRedisTemplate.opsForZSet()
                    .incrementScore(SocialRedisKey.POST_HOT_RANK, String.valueOf(targetId), weight);
        } catch (Exception e) {
            log.warn("HotScoreStreamListener.onMessage failed", e);
        }
    }

    private double weightOf(String type) {
        if (type == null) return 0;
        switch (type) {
            case InteractionEvent.TYPE_LIKE:
                return 3;
            case InteractionEvent.TYPE_UNLIKE:
                return -3;
            case InteractionEvent.TYPE_FAVORITE:
                return 5;
            case InteractionEvent.TYPE_UNFAVORITE:
                return -5;
            case InteractionEvent.TYPE_COMMENT:
                return 4;
            case InteractionEvent.TYPE_POST_VIEW:
                return 1;
            case InteractionEvent.TYPE_POST_PUBLISH:
                return 10;
            default:
                return 0;
        }
    }

    private Long parseLong(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer parseInt(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }
}
