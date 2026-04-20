package com.xcw.picturebackend.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.model.entity.Post;
import com.xcw.picturebackend.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 帖子热榜重算任务
 * - 流式消费只做增量；周期性地基于 DB 真实 count 做一次"全量快照"，避免 ZSET 长期漂移
 * - 计算公式：heat = likeCount*3 + favoriteCount*5 + commentCount*4 + viewCount*0.2
 *            + 时间衰减项（越新越高）
 * - 仅对近 30 天的公开帖参与榜单
 */
@Slf4j
@Component
public class PostHotScoreJob {

    private static final int TOP_CANDIDATE = 500;
    private static final long WINDOW_MILLIS = 30L * 24 * 3600 * 1000;

    @Resource
    private PostService postService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 每 10 分钟执行一次
     */
    @Scheduled(fixedDelay = 10L * 60 * 1000, initialDelay = 30L * 1000)
    public void recompute() {
        try {
            long now = System.currentTimeMillis();
            Date since = new Date(now - WINDOW_MILLIS);

            List<Post> list = postService.list(new LambdaQueryWrapper<Post>()
                    .eq(Post::getStatus, 1)
                    .eq(Post::getReviewStatus, 1)
                    .eq(Post::getVisibility, 0)
                    .ge(Post::getCreateTime, since)
                    .orderByDesc(Post::getCreateTime)
                    .last("LIMIT " + TOP_CANDIDATE));
            if (list.isEmpty()) return;

            Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>(list.size() * 2);
            for (Post p : list) {
                double score = scoreOf(p, now);
                tuples.add(ZSetOperations.TypedTuple.of(String.valueOf(p.getId()), score));
            }

            // 先删旧榜（仅保留最新一次快照，避免逐条 ZADD 累计漂移）
            stringRedisTemplate.delete(SocialRedisKey.POST_HOT_RANK);
            stringRedisTemplate.opsForZSet().add(SocialRedisKey.POST_HOT_RANK, tuples);
            // 榜单自身 7 天过期，避免任务长期停摆时数据一直不清理
            stringRedisTemplate.expire(SocialRedisKey.POST_HOT_RANK, java.time.Duration.ofDays(7));

            // 同步 hotScore 到 DB（仅 top 100）
            tuples.stream()
                    .sorted((a, b) -> Double.compare(
                            b.getScore() == null ? 0 : b.getScore(),
                            a.getScore() == null ? 0 : a.getScore()))
                    .limit(100)
                    .forEach(t -> {
                        try {
                            postService.lambdaUpdate()
                                    .eq(Post::getId, Long.parseLong(t.getValue()))
                                    .set(Post::getHotScore, t.getScore())
                                    .update();
                        } catch (Exception ignored) {
                        }
                    });

            log.info("PostHotScoreJob.recompute done. size={}", tuples.size());
        } catch (Exception e) {
            log.warn("PostHotScoreJob.recompute failed", e);
        }
    }

    private double scoreOf(Post p, long now) {
        long like = nz(p.getLikeCount());
        long fav = nz(p.getFavoriteCount());
        long comment = nz(p.getCommentCount());
        long view = nz(p.getViewCount());
        long ageHour = Math.max(0L, (now - p.getCreateTime().getTime()) / (3600L * 1000));
        // 简化 reddit-like：score / (age+2)^1.5
        double raw = like * 3.0 + fav * 5.0 + comment * 4.0 + view * 0.2;
        return raw / Math.pow(ageHour + 2.0, 1.5);
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }
}
