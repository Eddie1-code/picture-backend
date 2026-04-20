package com.xcw.picturebackend.model.dto.interaction;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 我的收藏 / 用户收藏 分页查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FavoriteQueryRequest extends PageRequest implements Serializable {

    /**
     * 目标类型过滤：1-图片 3-空间；为空表示全部
     */
    private Integer targetType;

    private static final long serialVersionUID = 1L;
}
