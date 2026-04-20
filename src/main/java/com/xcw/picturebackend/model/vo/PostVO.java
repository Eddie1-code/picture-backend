package com.xcw.picturebackend.model.vo;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xcw.picturebackend.model.entity.Post;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 帖子对外展示 VO
 */
@Data
public class PostVO implements Serializable {

    private Long id;

    private Long userId;

    private String content;

    /** 图片 url 列表（已解析） */
    private List<String> imageUrls;

    /** 标签列表（已解析） */
    private List<String> tags;

    private String location;

    private Integer visibility;

    private Integer allowComment;

    private Integer allowLike;

    private Integer allowCollect;

    private Long likeCount;

    private Long commentCount;

    private Long favoriteCount;

    private Long viewCount;

    private Long shareCount;

    private Double hotScore;

    private Integer reviewStatus;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    /** 作者信息 */
    private UserVO user;

    /** 当前登录用户是否已点赞 */
    private Boolean isLiked;

    /** 当前登录用户是否已收藏 */
    private Boolean isFavorite;

    /** 当前登录用户是否已关注作者 */
    private Boolean isFollowingAuthor;

    private static final long serialVersionUID = 1L;

    /** entity → vo */
    public static PostVO objToVo(Post post) {
        if (post == null) {
            return null;
        }
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        vo.setImageUrls(parseJsonArray(post.getImageUrls()));
        vo.setTags(parseJsonArray(post.getTags()));
        return vo;
    }

    private static List<String> parseJsonArray(String raw) {
        if (StrUtil.isBlank(raw)) {
            return java.util.Collections.emptyList();
        }
        try {
            return JSONUtil.toList(raw, String.class);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
