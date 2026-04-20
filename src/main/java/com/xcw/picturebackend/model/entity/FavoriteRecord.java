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
 * 通用收藏表
 *
 * @TableName favorite_record
 */
@TableName(value = "favorite_record")
@Data
public class FavoriteRecord implements Serializable {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 收藏用户 ID
     */
    private Long userId;

    /**
     * 被收藏内容 ID
     */
    private Long targetId;

    /**
     * 内容类型：1-图片 2-帖子 3-空间
     */
    private Integer targetType;

    /**
     * 被收藏内容所属用户 ID
     */
    private Long targetUserId;

    /**
     * 是否收藏：1-已收藏 0-已取消
     */
    private Integer isFavorite;

    /**
     * 收藏时间
     */
    private Date favoriteTime;

    /**
     * 是否已读
     */
    private Integer isRead;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
