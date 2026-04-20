package com.xcw.picturebackend.model.dto.post;

import com.xcw.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 帖子查询请求
 * - 全局列表 feed：不传 userId；只展示 visibility=0 & reviewStatus=1 & status=1
 * - 用户主页「帖子」Tab：传 userId，后端按「是否本人 / 是否粉丝」过滤 visibility
 * - 作者端「我的帖子」：不传；Controller 路径走 /post/my/list，直接用 loginUser.id
 * - 排序：latest(默认, createTime DESC)、hot(hotScore DESC, createTime DESC)
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PostQueryRequest extends PageRequest implements Serializable {

    /** 精确作者 id */
    private Long userId;

    /** 全文关键词（匹配 content） */
    private String searchText;

    /** 标签（单个） */
    private String tag;

    /** latest / hot */
    private String orderBy;

    /** 审核状态：null 表示默认（1-已通过） */
    private Integer reviewStatus;

    private static final long serialVersionUID = 1L;
}
