package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取候选图（预览）
 */
@Data
public class PictureBatchFetchCandidateVO implements Serializable {

    /**
     * 序号（从 1 开始）
     */
    private Integer index;

    /**
     * 图片直链
     */
    private String fileUrl;

    private static final long serialVersionUID = 1L;
}
