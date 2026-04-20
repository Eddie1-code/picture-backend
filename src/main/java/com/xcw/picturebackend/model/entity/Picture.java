package com.xcw.picturebackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value ="picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调（十六进制颜色值）
     */
    private String picColor;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;

    /**
     * 审核时间
     */
    private Date reviewTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    // ========== 社交互动字段 ==========

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 收藏数
     */
    private Long favoriteCount;

    /**
     * 浏览量
     */
    private Long viewCount;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 是否允许点赞：1-允许 0-禁止
     */
    private Integer allowLike;

    /**
     * 是否允许评论：1-允许 0-禁止
     */
    private Integer allowComment;

    /**
     * 是否允许收藏：1-允许 0-禁止
     */
    private Integer allowCollect;

    /**
     * 是否允许分享：1-允许 0-禁止
     */
    private Integer allowShare;

    /**
     * 热榜分数
     */
    private Double hotScore;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}

