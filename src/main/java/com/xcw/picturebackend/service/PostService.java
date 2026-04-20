package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.post.PostAddRequest;
import com.xcw.picturebackend.model.dto.post.PostEditRequest;
import com.xcw.picturebackend.model.dto.post.PostQueryRequest;
import com.xcw.picturebackend.model.entity.Post;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.PostVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 帖子 / 动态服务
 */
public interface PostService extends IService<Post> {

    /** 发帖 */
    Long addPost(PostAddRequest request, User loginUser);

    /** 编辑帖子（仅作者 / 管理员） */
    boolean editPost(PostEditRequest request, User loginUser);

    /** 删除帖子（仅作者 / 管理员） */
    boolean deletePost(Long postId, User loginUser);

    /** 获取单条（带权限、是否已点赞/收藏/关注作者） */
    PostVO getPostVO(Long postId, HttpServletRequest httpServletRequest);

    /** 全局 Feed 或指定用户的帖子列表 */
    Page<PostVO> listPostVOByPage(PostQueryRequest request, HttpServletRequest httpServletRequest);

    /** 增加浏览量（1 次） */
    void increaseViewCount(Long postId);
}
