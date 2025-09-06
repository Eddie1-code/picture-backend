package com.xcw.picturebackend.model.dto.picture;


/**
 * @author 2340129326 许灿炜
 * @date 2025/9/6
 */

import lombok.Data;

import java.io.Serializable;

/**
 * 图片批量上传请求
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

    private static final long serialVersionUID = 1L;
}

