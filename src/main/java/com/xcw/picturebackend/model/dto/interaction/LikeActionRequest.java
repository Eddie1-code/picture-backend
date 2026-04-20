package com.xcw.picturebackend.model.dto.interaction;

import lombok.Data;

import java.io.Serializable;

/**
 * 点赞 / 取消点赞 请求
 */
@Data
public class LikeActionRequest implements Serializable {

    /**
     * 目标 ID（图片/空间/帖子 id）
     */
    private Long targetId;

    /**
     * 目标类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    private static final long serialVersionUID = 1L;
}
