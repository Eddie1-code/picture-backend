package com.xcw.picturebackend.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xcw.picturebackend.model.vo.PostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 帖子模块 Cache-Aside 管理器
 * - 热点数据：帖子详情 / 论坛列表首页结果
 * - 策略：Caffeine 本地 + Redis 二级；空结果允许短 TTL 缓存防穿透
 * - 更新：延迟双删（立即删一次，2 秒后再删一次），消化并发窗口写入的脏缓存
 */
@Slf4j
@Component
public class PostCacheManager {

    /** 详情缓存 Key 前缀 */
    private static final String KEY_DETAIL = "picture:post:detail:vo:";
    /** 列表缓存 Key 前缀 */
    private static final String KEY_LIST = "picture:post:list:vo:";
    /** 详情 TTL 基础值（秒） */
    private static final int DETAIL_TTL_BASE = 300;
    /** 列表 TTL 基础值（秒） */
    private static final int LIST_TTL_BASE = 180;
    /** 空值占位（避免穿透） */
    private static final String NULL_HOLDER = "__NULL__";
    /** 空值 TTL（秒） */
    private static final int NULL_TTL = 60;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 本地 L1 缓存 */
    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .initialCapacity(512)
            .maximumSize(5000L)
            .expireAfterWrite(2L, TimeUnit.MINUTES)
            .build();

    /** 延迟二次删除用的调度线程 */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "post-cache-evictor");
        t.setDaemon(true);
        return t;
    });

    // ======================== 详情缓存 ========================

    public String detailKey(Long postId) {
        return KEY_DETAIL + postId;
    }

    public PostVO getDetail(Long postId) {
        String key = detailKey(postId);
        String val = localCache.getIfPresent(key);
        if (val == null) {
            try {
                val = stringRedisTemplate.opsForValue().get(key);
                if (val != null) {
                    localCache.put(key, val);
                }
            } catch (Exception e) {
                log.warn("PostCacheManager.getDetail redis failed, id={}", postId, e);
            }
        }
        if (val == null) return null;
        if (NULL_HOLDER.equals(val)) return null; // 空值命中，当作未找到
        return JSONUtil.toBean(val, PostVO.class);
    }

    /**
     * 专门的空命中标记：缓存穿透防护
     */
    public boolean isNullCached(Long postId) {
        String key = detailKey(postId);
        String val = localCache.getIfPresent(key);
        if (val == null) {
            try {
                val = stringRedisTemplate.opsForValue().get(key);
            } catch (Exception ignored) {
            }
        }
        return NULL_HOLDER.equals(val);
    }

    public void putDetail(Long postId, PostVO vo) {
        String key = detailKey(postId);
        String json = JSONUtil.toJsonStr(vo);
        int ttl = DETAIL_TTL_BASE + RandomUtil.randomInt(0, 120);
        try {
            stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("PostCacheManager.putDetail redis failed, id={}", postId, e);
        }
        localCache.put(key, json);
    }

    public void putDetailNull(Long postId) {
        String key = detailKey(postId);
        try {
            stringRedisTemplate.opsForValue().set(key, NULL_HOLDER, NULL_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("PostCacheManager.putDetailNull failed, id={}", postId, e);
        }
        localCache.put(key, NULL_HOLDER);
    }

    /**
     * 延迟双删：立即删一次 + 2 秒后再删一次。建议在写路径事务提交后调用。
     */
    public void invalidateDetail(Long postId) {
        String key = detailKey(postId);
        evict(key);
        scheduler.schedule(() -> evict(key), 2L, TimeUnit.SECONDS);
    }

    // ======================== 列表缓存 ========================

    public String listKey(Object queryRequest) {
        String queryCondition = JSONUtil.toJsonStr(queryRequest);
        String hash = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        return KEY_LIST + hash;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Page<PostVO> getList(String key) {
        String val = localCache.getIfPresent(key);
        if (val == null) {
            try {
                val = stringRedisTemplate.opsForValue().get(key);
                if (val != null) {
                    localCache.put(key, val);
                }
            } catch (Exception e) {
                log.warn("PostCacheManager.getList redis failed, key={}", key, e);
            }
        }
        if (val == null || NULL_HOLDER.equals(val)) return null;
        return JSONUtil.toBean(val, Page.class);
    }

    public void putList(String key, Page<PostVO> page) {
        String json = JSONUtil.toJsonStr(page);
        int ttl = LIST_TTL_BASE + RandomUtil.randomInt(0, 120);
        try {
            stringRedisTemplate.opsForValue().set(key, json, ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("PostCacheManager.putList redis failed, key={}", key, e);
        }
        localCache.put(key, json);
    }

    /**
     * 列表键数量不可枚举，写路径用 pattern 清扫代价大——
     * 这里采用「版本号」方式：读路径会把版本号纳入 key 生成，写路径仅 incr 版本号即可批量失效。
     * 当前 listKey 只 hash 了请求本体，如果后续需要完整失效可基于此扩展。
     */
    public void invalidateAllLists() {
        try {
            // 简单实现：扫描 prefix 并批量删除，仅在运维级热更新时使用
            var keys = stringRedisTemplate.keys(KEY_LIST + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("PostCacheManager.invalidateAllLists failed", e);
        }
        localCache.invalidateAll();
    }

    private void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("PostCacheManager.evict redis failed, key={}", key, e);
        }
        localCache.invalidate(key);
    }
}
