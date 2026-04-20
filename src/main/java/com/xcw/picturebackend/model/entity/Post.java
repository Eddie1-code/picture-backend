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
 * 帖子 / 动态
 *
 * @TableName t_post
 */
@TableName(value = "t_post")
@Data
public class Post implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 作者 */
    private Long userId;

    /** 正文（纯文本，建议 1000 字以内） */
    private String content;

    /** 配图 url 数组，JSON 字符串，最多 9 张 */
    private String imageUrls;

    /** 标签 JSON 数组字符串 */
    private String tags;

    /** 地理位置（省份） */
    private String location;

    /** 0-公开 1-仅粉丝 2-仅自己 */
    private Integer visibility;

    /** 是否允许评论 */
    private Integer allowComment;

    /** 是否允许点赞 */
    private Integer allowLike;

    /** 是否允许收藏 */
    private Integer allowCollect;

    /** 点赞数（冗余） */
    private Long likeCount;

    /** 评论数 */
    private Long commentCount;

    /** 收藏数 */
    private Long favoriteCount;

    /** 浏览量 */
    private Long viewCount;

    /** 分享数 */
    private Long shareCount;

    /** 热榜分 */
    private Double hotScore;

    /** 0-待审核 1-通过 2-拒绝 */
    private Integer reviewStatus;

    /** 审核意见 */
    private String reviewMessage;

    /** 审核人 */
    private Long reviewerId;

    /** 审核时间 */
    private Date reviewTime;

    /** 0-草稿 1-已发布 2-已下架 */
    private Integer status;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
