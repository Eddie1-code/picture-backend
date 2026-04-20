package com.xcw.picturebackend.model.vo;

import com.xcw.picturebackend.model.entity.Comments;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 评论视图对象（携带用户信息 + 子回复预览）
 */
@Data
public class CommentVO implements Serializable {

    private Long commentId;

    private Long userId;

    private Long targetId;

    private Integer targetType;

    private Long targetUserId;

    private String content;

    private Long parentCommentId;

    private Long rootCommentId;

    private Long replyToUserId;

    private Long likeCount;

    private Long dislikeCount;

    private String location;

    private Date createTime;

    private Date updateTime;

    /**
     * 评论用户信息
     */
    private UserVO user;

    /**
     * 被回复的用户信息（仅回复评论时有）
     */
    private UserVO replyToUser;

    /**
     * 当前用户是否已点赞该评论
     */
    private Boolean isLiked;

    /**
     * 子回复数（针对顶级评论）
     */
    private Long childCount;

    /**
     * 子回复预览（按时间正序前 N 条），可为 null
     */
    private List<CommentVO> childComments;

    private static final long serialVersionUID = 1L;

    public static CommentVO objToVo(Comments comments) {
        if (comments == null) {
            return null;
        }
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comments, vo);
        vo.setChildComments(new ArrayList<>());
        return vo;
    }
}
