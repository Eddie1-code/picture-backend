package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 消息中心未读数聚合
 */
@Data
public class NotifyUnreadVO implements Serializable {

    private Long likeCount;

    private Long commentCount;

    private Long favoriteCount;

    private Long followCount;

    private Long systemCount;

    private Long totalCount;

    private static final long serialVersionUID = 1L;
}
