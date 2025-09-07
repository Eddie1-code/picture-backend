package com.xcw.picturebackend.manager;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.vo.PictureVO;
import com.xcw.picturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PictureCacheManager {

    @Resource
    @Lazy
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 构建缓存key
     */
    public String buildCacheKey(Object queryRequest) {
        String queryCondition = cn.hutool.json.JSONUtil.toJsonStr(queryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        return String.format("picture:listPictureVOByPage:%s", hashKey);
    }

    /**
     * 从缓存获取分页数据
     */
    public Page<PictureVO> getPageFromCache(String cacheKey) {
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return JSONUtil.toBean(cachedValue, Page.class);
        }
        return null;
    }


    /**
     * 写入缓存
     */
    public void putPageToCache(String cacheKey, Page<PictureVO> page) {
        String cacheValue = JSONUtil.toJsonStr(page);
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        LOCAL_CACHE.put(cacheKey, cacheValue);
    }

    private static final long HOT_THRESHOLD = 100; // 热点阈值，可根据实际调整


    // 定时检测热点图片并加入本地缓存
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void detectAndCacheHotPictures() {
        Set<Long> pictureIds = pictureService.list().stream().map(Picture::getId).collect(java.util.stream.Collectors.toSet());
        for (Long pictureId : pictureIds) {
            String key = "picture:access:" + pictureId;
            String countStr = stringRedisTemplate.opsForValue().get(key);
            long count = countStr == null ? 0 : Long.parseLong(countStr);
            if (count > HOT_THRESHOLD) {
                Picture picture = pictureService.getById(pictureId);
                if (picture != null) {
                    PictureVO pictureVO = PictureVO.objToVo(picture);
                    // 只在首次加入热点缓存时输出
                    if (LOCAL_CACHE.getIfPresent("picture:hot:" + pictureId) == null) {
                        log.info("已加入本地热点缓存: picture:hot:{}", pictureId);
                    }
                    LOCAL_CACHE.put("picture:hot:" + pictureId, JSONUtil.toJsonStr(pictureVO));
                }
            }
        }
    }

    public PictureVO getPictureWithHotCache(String pictureId) {
        String cacheKey = "picture:hot:" + pictureId;
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            log.debug("本地热点缓存命中: " + cacheKey);
            return JSONUtil.toBean(cachedValue, PictureVO.class);
        }
        // 未命中则查数据库
        Picture picture = pictureService.getById(Long.parseLong(pictureId));
        if (picture != null) {
            return PictureVO.objToVo(picture);
        }
        return null;
    }

    // 统计图片访问量
    public void recordPictureAccess(String pictureId) {
        String key = "picture:access:" + pictureId;
        stringRedisTemplate.opsForValue().increment(key);
    }


}