package com.xcw.picturebackend.model.dto.interaction;

import lombok.Data;

import java.io.Serializable;

/**
 * 收藏 / 取消收藏 请求
 */
@Data
public class FavoriteActionRequest implements Serializable {

    private Long targetId;

    /**
     * 目标类型：1-图片 3-空间
     */
    private Integer targetType;

    private static final long serialVersionUID = 1L;
}
