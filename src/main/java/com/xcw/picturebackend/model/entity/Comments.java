package com.xcw.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通用评论表（两级：评论 + 回复）
 *
 * @TableName comments
 */
@TableName(value = "comments")
@Data
public class Comments implements Serializable {

    @TableId(value = "commentId", type = IdType.ASSIGN_ID)
    private Long commentId;

    /**
     * 评论用户 ID
     */
    private Long userId;

    /**
     * 评论目标 ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 目标所属用户 ID（作者）
     */
    private Long targetUserId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 父评论 ID（0=顶级评论）
     */
    private Long parentCommentId;

    /**
     * 根评论 ID
     */
    private Long rootCommentId;

    /**
     * 被回复的用户 ID
     */
    private Long replyToUserId;

    /**
     * 评论点赞数
     */
    private Long likeCount;

    /**
     * 评论点踩数
     */
    private Long dislikeCount;

    /**
     * 评论位置（省份）
     */
    private String location;

    /**
     * 是否已读
     */
    private Integer isRead;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
