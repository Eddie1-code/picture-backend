package com.xcw.picturebackend.model.dto.post;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发帖请求
 * - content / imageUrls 至少二选一（前端可只发图或只发文字）
 * - imageUrls 由 FileController 先上传得到 url 后，前端再提交
 */
@Data
public class PostAddRequest implements Serializable {

    /** 正文 */
    private String content;

    /** 配图 url 列表，最多 9 张 */
    private List<String> imageUrls;

    /** 标签列表 */
    private List<String> tags;

    /** 地理位置（省份） */
    private String location;

    /** 0-公开 1-仅粉丝 2-仅自己；默认 0 */
    private Integer visibility;

    /** 是否允许评论（1-允许 0-禁止，默认 1） */
    private Integer allowComment;

    /** 是否允许点赞（默认 1） */
    private Integer allowLike;

    /** 是否允许收藏（默认 1） */
    private Integer allowCollect;

    /** 客户端幂等 id（配合 Redis 锁） */
    private String clientReqId;

    private static final long serialVersionUID = 1L;
}
