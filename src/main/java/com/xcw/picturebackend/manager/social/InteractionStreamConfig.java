package com.xcw.picturebackend.manager.social;

import com.xcw.picturebackend.constant.SocialRedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.net.InetAddress;
import java.time.Duration;

/**
 * 互动事件 Stream 消费者容器
 * - 在应用启动时确保消费者组存在（XGROUP CREATE MKSTREAM）
 * - 注册三个消费者组：g-notify / g-hotscore / g-stat
 * - 三个消费者对同一 Stream 并行消费，互不影响（消费者组语义）
 */
@Slf4j
@Configuration
public class InteractionStreamConfig {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private NotifyStreamListener notifyStreamListener;

    @Autowired
    private HotScoreStreamListener hotScoreStreamListener;

    @Autowired
    private StatStreamListener statStreamListener;

    /**
     * 启动时确保 Stream + 消费者组存在
     */
    private void ensureGroup(String group) {
        try {
            stringRedisTemplate.opsForStream()
                    .createGroup(SocialRedisKey.STREAM_INTERACTION, ReadOffset.from("0"), group);
        } catch (Exception e) {
            // 已存在时会抛 BUSYGROUP，忽略即可
            log.debug("createGroup {} on {} ignored: {}",
                    group, SocialRedisKey.STREAM_INTERACTION, e.getMessage());
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> interactionStreamContainer(
            RedisConnectionFactory factory
    ) {
        ensureGroup(SocialRedisKey.STREAM_GROUP_NOTIFY);
        ensureGroup(SocialRedisKey.STREAM_GROUP_HOTSCORE);
        ensureGroup(SocialRedisKey.STREAM_GROUP_STAT);

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<
                String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofSeconds(5))
                        .batchSize(16)
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(factory, options);

        String host = hostname();

        Subscription notifySub = container.receiveAutoAck(
                Consumer.from(SocialRedisKey.STREAM_GROUP_NOTIFY, host + "-notify"),
                StreamOffset.create(SocialRedisKey.STREAM_INTERACTION, ReadOffset.lastConsumed()),
                notifyStreamListener
        );

        Subscription hotSub = container.receiveAutoAck(
                Consumer.from(SocialRedisKey.STREAM_GROUP_HOTSCORE, host + "-hotscore"),
                StreamOffset.create(SocialRedisKey.STREAM_INTERACTION, ReadOffset.lastConsumed()),
                hotScoreStreamListener
        );

        Subscription statSub = container.receiveAutoAck(
                Consumer.from(SocialRedisKey.STREAM_GROUP_STAT, host + "-stat"),
                StreamOffset.create(SocialRedisKey.STREAM_INTERACTION, ReadOffset.lastConsumed()),
                statStreamListener
        );

        log.info("InteractionStream subscriptions started: {} / {} / {}",
                notifySub, hotSub, statSub);
        return container;
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "host-" + System.currentTimeMillis();
        }
    }
}
