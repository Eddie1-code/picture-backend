package com.xcw.picturebackend.manager;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.xcw.picturebackend.config.CosClientConfig;
import com.xcw.picturebackend.model.entity.Picture;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {

    /** 列表/卡片用缩略图最长边（原 128 过小，首页网格放大后严重模糊） */
    private static final int THUMBNAIL_MAX_EDGE = 640;
    /** WebP 质量 1–100，兼顾体积与清晰度（默认无 quality 时偏糊） */
    private static final int WEBP_QUALITY = 82;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传对象（支持显式设置 Content-Type）
     *
     * @param key         唯一键
     * @param file        文件
     * @param contentType 文件内容类型，例如 image/jpeg
     */
    public PutObjectResult putObject(String key, File file, String contentType) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        if (StrUtil.isNotBlank(contentType)) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentType(contentType);
            putObjectRequest.setMetadata(objectMetadata);
        }
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);

        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();
        // 图片压缩（WebP + 明确质量，避免过度压缩发糊）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setRule(String.format("imageMogr2/format/webp/quality/%d", WEBP_QUALITY));
        compressRule.setBucket(cosClientConfig.getBucket());
        rules.add(compressRule);

        // 缩略图：限制最长边，供列表/首页卡片使用（与 PictureTile 的 thumbnailUrl 对应）
        if (file.length() > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." /*+ FileUtil.getSuffix(key) */;
            thumbnailRule.setFileId(thumbnailKey);
            // thumbnail/WxH>：等比缩放，宽高不超过 W、H；quality 与主图一致
            thumbnailRule.setRule(
                    String.format(
                            "imageMogr2/thumbnail/%dx%d>/quality/%d",
                            THUMBNAIL_MAX_EDGE,
                            THUMBNAIL_MAX_EDGE,
                            WEBP_QUALITY));
            rules.add(thumbnailRule);
        }

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws CosClientException {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}

