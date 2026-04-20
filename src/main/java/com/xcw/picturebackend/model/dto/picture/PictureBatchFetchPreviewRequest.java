package com.xcw.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取预览：仅解析候选图片 URL，不上传
 */
@Data
public class PictureBatchFetchPreviewRequest implements Serializable {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 期望候选数量（最大 30）
     */
    private Integer count = 20;

    private static final long serialVersionUID = 1L;
}
