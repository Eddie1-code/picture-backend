package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 「我的收藏」列表项，目标可能是图片/空间，携带最少必要字段
 */
@Data
public class FavoriteVO implements Serializable {

    private Long favoriteRecordId;

    private Long targetId;

    /**
     * 1-图片 3-空间
     */
    private Integer targetType;

    private Date favoriteTime;

    /**
     * 图片封装（targetType=1 时非空）
     */
    private PictureVO picture;

    /**
     * 空间封装（targetType=3 时非空）
     */
    private SpaceVO space;

    private static final long serialVersionUID = 1L;
}
