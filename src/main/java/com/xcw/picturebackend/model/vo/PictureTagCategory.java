package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签和分类信息视图
 * 用于前端展示图片的标签和分类
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
