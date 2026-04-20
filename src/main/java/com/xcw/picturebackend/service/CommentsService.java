package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.comment.CommentAddRequest;
import com.xcw.picturebackend.model.dto.comment.CommentQueryRequest;
import com.xcw.picturebackend.model.entity.Comments;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.CommentVO;

/**
 * 通用评论服务
 */
public interface CommentsService extends IService<Comments> {

    /**
     * 新增评论 / 回复，返回已保存的 VO
     */
    CommentVO addComment(CommentAddRequest request, User loginUser);

    /**
     * 删除评论（作者本人 or 被评论内容作者 or 管理员）；软删除，级联删除子回复
     */
    boolean deleteComment(Long commentId, User loginUser);

    /**
     * 分页拉顶级评论，并为每条携带前 N 条子回复预览
     */
    Page<CommentVO> listTopLevelComments(CommentQueryRequest request, User loginUser);

    /**
     * 分页拉某一根评论下的所有子回复（楼中楼展开）
     */
    Page<CommentVO> listChildComments(Long rootCommentId, long current, long size, User loginUser);
}
