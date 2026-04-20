package com.xcw.picturebackend.model.dto.comment;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 评论分页查询：按目标（targetId + targetType）拉顶级评论
 * 每条顶级评论额外带前 N 条子回复（N 在 service 里控制）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentQueryRequest extends PageRequest implements Serializable {

    private Long targetId;

    private Integer targetType;

    private static final long serialVersionUID = 1L;
}
