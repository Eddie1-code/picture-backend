package com.xcw.picturebackend;


import com.xcw.picturebackend.manager.PictureCacheManager;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.vo.PictureVO;
import com.xcw.picturebackend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/7
 */
@SpringBootTest
public class PictureHotCacheTest {

    @Resource
    private PictureService pictureService;

    @Resource
    private PictureCacheManager pictureCacheManager;

    @Test
    public void testHotCache() throws InterruptedException {
        // 1.测试图片ID
        Long testId = 1958072475594108929L; // 替换为实际图片ID
        Picture picture = pictureService.getById(testId);
        // 2.模拟多次访问，超过热点阈值（设置了100次）
        for (int i = 0; i < 120; i++) {
            pictureService.getPictureVO(picture, null);
        }
        // 3.等待定时任务执行（定时任务每分钟执行一次）
        Thread.sleep(65000);
        // 4.检查本地缓存是否命中
        PictureVO vo = pictureCacheManager.getPictureWithHotCache(testId.toString());
        assertNotNull(vo);
    }
}
