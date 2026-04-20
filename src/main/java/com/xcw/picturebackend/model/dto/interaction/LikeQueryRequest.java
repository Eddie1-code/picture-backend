package com.xcw.picturebackend.model.dto.interaction;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 「我 / 某用户」的点赞记录分页查询请求。
 * 查看他人列表时，服务端会依据 {@code user.showLikeList} 校验隐私。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LikeQueryRequest extends PageRequest implements Serializable {

    /**
     * 目标类型过滤：1-图片 2-帖子；为空表示全部
     */
    private Integer targetType;

    /**
     * 可选：查询指定用户的点赞列表（为空或等于当前登录用户则为自己）
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
