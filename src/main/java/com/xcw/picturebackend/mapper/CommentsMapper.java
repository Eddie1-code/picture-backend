package com.xcw.picturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcw.picturebackend.model.entity.Comments;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 通用评论表 Mapper
 */
public interface CommentsMapper extends BaseMapper<Comments> {

    /**
     * 统计一组根评论各自的子回复条数（只统计未删除的回复）
     *
     * @param rootCommentIds 根评论 id 列表
     * @return [{rootCommentId, cnt}]
     */
    List<Map<String, Object>> countChildrenByRoot(@Param("rootCommentIds") Collection<Long> rootCommentIds);

    /**
     * 查询每个根评论最新前 N 条子回复（默认 2 条），正序返回供前端预览。
     * 使用窗口函数兼容 MySQL 8+。
     */
    List<Comments> listChildPreviewByRoot(@Param("rootCommentIds") Collection<Long> rootCommentIds,
                                          @Param("limit") int limit);
}
