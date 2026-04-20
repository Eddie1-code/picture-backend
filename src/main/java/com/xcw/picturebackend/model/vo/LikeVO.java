package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 「某用户的点赞」列表项。
 * 目前主要承载图片（targetType=1）；帖子（targetType=2）在引入帖子模块后续填充。
 */
@Data
public class LikeVO implements Serializable {

    private Long likeRecordId;

    private Long targetId;

    /**
     * 1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    private Date likeTime;

    /**
     * 图片封装（targetType=1 时非空）
     */
    private PictureVO picture;

    /**
     * 帖子封装（targetType=2 时非空）
     */
    private PostVO post;

    private static final long serialVersionUID = 1L;
}
