package com.xcw.picturebackend.model.dto.space;


import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author 2340129326 许灿炜
 * @date 2025/9/11
 */

/**
 * 空间级别
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 级别值
     */
    private int value;

    /**
     * 级别描述
     */
    private String text;

    /**
     * 最大图片数量
     */
    private long maxCount;

    /**
     * 最大存储容量
     */
    private long maxSize;
}
