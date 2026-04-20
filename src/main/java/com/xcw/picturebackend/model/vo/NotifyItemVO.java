package com.xcw.picturebackend.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息中心统一的消息条目
 */
@Data
public class NotifyItemVO implements Serializable {

    /**
     * 业务唯一 ID（评论 ID / 点赞记录 ID / 收藏记录 ID / 关注记录 ID / 系统通知 ID）
     */
    private Long bizId;

    /**
     * 类型：like/comment/favorite/follow/system
     */
    private String notifyType;

    /**
     * 是否已读
     */
    private Boolean isRead;

    /**
     * 发生时间
     */
    private Date notifyTime;

    /**
     * 操作人 / 发送人 摘要（发起点赞/评论/关注/收藏的用户）
     */
    private UserVO fromUser;

    /**
     * 文案（例如："赞了你的图片" / "评论了你的图片：xxx" / "关注了你"）
     */
    private String text;

    /**
     * 关联目标类型：1-图片 3-空间；系统通知为 null
     */
    private Integer targetType;

    /**
     * 关联目标 ID
     */
    private Long targetId;

    /**
     * 关联目标摘要（如图片 name / 空间名 / 系统通知标题）
     */
    private String targetTitle;

    /**
     * 关联目标封面图（仅图片/空间）
     */
    private String targetCover;

    /**
     * 系统通知扩展字段：标题 / 图标 / 跳转业务类型+ID
     */
    private String title;

    private String icon;

    private String relatedBizType;

    private String relatedBizId;

    private static final long serialVersionUID = 1L;
}
