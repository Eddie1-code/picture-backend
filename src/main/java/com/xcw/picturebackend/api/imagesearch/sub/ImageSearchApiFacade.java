package com.xcw.picturebackend.api.imagesearch.sub;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/14
 */

import com.xcw.picturebackend.api.imagesearch.model.GetImageListApi;
import com.xcw.picturebackend.api.imagesearch.model.GetImageUrlApi;
import com.xcw.picturebackend.api.imagesearch.model.ImageSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 360搜图图片搜索接口
 * <p>
 * 这里用了 门面模式
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl 需要以图搜图的图片地址
     * @param start    开始下表
     * @return 图片搜索结果列表
     */
    public static List<ImageSearchResult> searchImage(String imageUrl, Integer start) {
        String soImageUrl = GetImageUrlApi.getSoImageUrl(imageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(soImageUrl, start);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "https://baolong-picture-1259638363.cos.ap-shanghai.myqcloud.com//public/10000000/2025-02-15_lzn23PuxZqt8CPB1.";
        List<ImageSearchResult> resultList = searchImage(imageUrl, 0);
        System.out.println("图片搜索结果：");
        for (ImageSearchResult result : resultList) {
            // 假设 SoImageSearchResult 有 getTitle() 和 getImageUrl() 方法
            System.out.println("标题：" + result.getTitle());
            System.out.println("图片地址：" + result.getImgUrl());
            System.out.println("----------------------");
        }
    }

}
