package com.xcw.picturebackend.model.dto.space;


import lombok.Data;

import java.io.Serializable;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/10
 */

/**
 * 更新空间请求
 */
@Data
public class SpaceUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    /**
     * 空间图片的最大总大小
     */
    private Long maxSize;

    /**
     * 空间图片的最大数量
     */
    private Long maxCount;

    private static final long serialVersionUID = 1L;
}