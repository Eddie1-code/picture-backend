package com.xcw.picturebackend.model.dto.comment;

import lombok.Data;

import java.io.Serializable;

/**
 * 新增评论 / 回复
 */
@Data
public class CommentAddRequest implements Serializable {

    /**
     * 目标 ID
     */
    private Long targetId;

    /**
     * 目标类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论 ID（0 或 null 表示顶级评论）
     */
    private Long parentCommentId;

    /**
     * 根评论 ID（顶级为 null；回复时必传，等于被回复评论所在话题的根 id）
     */
    private Long rootCommentId;

    /**
     * 被回复的用户 ID（@xxx），顶级评论可为空
     */
    private Long replyToUserId;

    private static final long serialVersionUID = 1L;
}
