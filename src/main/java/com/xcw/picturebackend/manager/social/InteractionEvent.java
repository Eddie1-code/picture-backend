package com.xcw.picturebackend.manager.social;

import lombok.Data;

/**
 * 互动事件统一模型
 * - eventType: LIKE / UNLIKE / FAVORITE / UNFAVORITE / COMMENT / FOLLOW / POST_VIEW / POST_PUBLISH
 * - actorId:  动作发起者（点赞的人 / 评论的人）
 * - targetUserId: 动作接收者 / 内容作者
 * - targetType: TargetTypeEnum，例如 2 表示 POST
 * - targetId:  目标业务 id（postId / pictureId 等）
 */
@Data
public class InteractionEvent {

    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_UNLIKE = "UNLIKE";
    public static final String TYPE_FAVORITE = "FAVORITE";
    public static final String TYPE_UNFAVORITE = "UNFAVORITE";
    public static final String TYPE_COMMENT = "COMMENT";
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_POST_VIEW = "POST_VIEW";
    public static final String TYPE_POST_PUBLISH = "POST_PUBLISH";

    private String eventType;
    private Long actorId;
    private Long targetUserId;
    private Integer targetType;
    private Long targetId;
    private Long ts;

    public static InteractionEvent of(String eventType, Long actorId, Long targetUserId,
                                      Integer targetType, Long targetId) {
        InteractionEvent e = new InteractionEvent();
        e.setEventType(eventType);
        e.setActorId(actorId);
        e.setTargetUserId(targetUserId);
        e.setTargetType(targetType);
        e.setTargetId(targetId);
        e.setTs(System.currentTimeMillis());
        return e;
    }
}
