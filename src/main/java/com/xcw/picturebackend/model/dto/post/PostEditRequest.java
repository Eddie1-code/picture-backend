package com.xcw.picturebackend.model.dto.post;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑帖子请求
 * - 仅作者本人 / 管理员可改
 * - 任意字段可不传（null 表示不更新）；但 imageUrls 需要整体替换
 */
@Data
public class PostEditRequest implements Serializable {

    private Long id;

    private String content;

    private List<String> imageUrls;

    private List<String> tags;

    private String location;

    private Integer visibility;

    private Integer allowComment;

    private Integer allowLike;

    private Integer allowCollect;

    private static final long serialVersionUID = 1L;
}
