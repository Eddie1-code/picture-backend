package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.constant.UserConstant;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.InteractionLockManager;
import com.xcw.picturebackend.mapper.CommentsMapper;
import com.xcw.picturebackend.model.dto.comment.CommentAddRequest;
import com.xcw.picturebackend.model.dto.comment.CommentQueryRequest;
import com.xcw.picturebackend.model.entity.Comments;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.model.vo.CommentVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.CommentsService;
import com.xcw.picturebackend.service.PictureService;
import com.xcw.picturebackend.service.SpaceService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用评论服务实现
 * - 两级评论：顶级（parentCommentId=0）+ 回复（parentCommentId=顶级id, rootCommentId=顶级id）
 * - 计数：顶级评论 +1/-1 作用于 picture.commentCount（回复不计入；保持与主流产品口径一致）
 */
@Slf4j
@Service
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService {

    private static final int COMMENT_MAX_LEN = 1000;
    private static final int CHILD_PREVIEW_COUNT = 2;
    private static final int COMMENT_MAX_PAGE_SIZE = 20;

    @Resource
    private InteractionLockManager interactionLockManager;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO addComment(CommentAddRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = loginUser.getId();

        Long targetId = request.getTargetId();
        Integer targetType = request.getTargetType();
        String content = request.getContent();

        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR, "目标 id 非法");
        ThrowUtils.throwIf(!TargetTypeEnum.isValid(targetType), ErrorCode.PARAMS_ERROR, "目标类型非法");
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        ThrowUtils.throwIf(content.length() > COMMENT_MAX_LEN, ErrorCode.PARAMS_ERROR,
                "评论内容过长（≤" + COMMENT_MAX_LEN + " 字）");

        if (!interactionLockManager.tryLockComment(userId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "评论太快啦，请稍后再试");
        }

        // 目标存在性 + 允许评论校验 + 目标作者
        Long targetUserId;
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            Picture p = pictureService.getById(targetId);
            ThrowUtils.throwIf(p == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (p.getAllowComment() != null && p.getAllowComment() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭评论");
            }
            targetUserId = p.getUserId();
        } else if (TargetTypeEnum.SPACE.getValue() == targetType) {
            Space s = spaceService.getById(targetId);
            ThrowUtils.throwIf(s == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            targetUserId = s.getUserId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该目标类型");
        }

        // 父评论 / 根评论校验（回复时）
        Long parentCommentId = request.getParentCommentId();
        Long rootCommentId = request.getRootCommentId();
        Long replyToUserId = request.getReplyToUserId();

        boolean isTopLevel = (parentCommentId == null || parentCommentId == 0L);
        if (isTopLevel) {
            parentCommentId = 0L;
            rootCommentId = null;
            replyToUserId = null;
        } else {
            Comments parent = this.getById(parentCommentId);
            ThrowUtils.throwIf(parent == null || Integer.valueOf(1).equals(parent.getIsDelete()),
                    ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
            ThrowUtils.throwIf(!parent.getTargetId().equals(targetId) || !parent.getTargetType().equals(targetType),
                    ErrorCode.PARAMS_ERROR, "父评论与目标不一致");
            // 只允许两级：若父评论已经是回复，则根评论继承父评论的根
            Long inferredRoot = parent.getRootCommentId() != null ? parent.getRootCommentId() : parent.getCommentId();
            if (rootCommentId == null) {
                rootCommentId = inferredRoot;
            } else if (!rootCommentId.equals(inferredRoot)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "根评论 id 与父评论不一致");
            }
            if (replyToUserId == null) {
                replyToUserId = parent.getUserId();
            }
        }

        Comments entity = new Comments();
        entity.setUserId(userId);
        entity.setTargetId(targetId);
        entity.setTargetType(targetType);
        entity.setTargetUserId(targetUserId);
        entity.setContent(content);
        entity.setParentCommentId(parentCommentId);
        entity.setRootCommentId(rootCommentId);
        entity.setReplyToUserId(replyToUserId);
        entity.setLikeCount(0L);
        entity.setDislikeCount(0L);
        entity.setIsRead(0);
        boolean ok = this.save(entity);
        ThrowUtils.throwIf(!ok, ErrorCode.OPERATION_ERROR, "评论失败");

        // 只在顶级评论时更新 picture.commentCount
        if (isTopLevel && TargetTypeEnum.PICTURE.getValue() == targetType) {
            pictureService.lambdaUpdate()
                    .eq(Picture::getId, targetId)
                    .setSql("commentCount = GREATEST(0, IFNULL(commentCount,0) + 1)")
                    .update();
        }

        // 构造 VO
        CommentVO vo = CommentVO.objToVo(entity);
        vo.setUser(userService.getUserVO(loginUser));
        if (replyToUserId != null) {
            User replyTo = userService.getById(replyToUserId);
            if (replyTo != null) {
                vo.setReplyToUser(userService.getUserVO(replyTo));
            }
        }
        vo.setIsLiked(false);
        vo.setChildCount(0L);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(Long commentId, User loginUser) {
        ThrowUtils.throwIf(commentId == null || commentId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Comments c = this.getById(commentId);
        ThrowUtils.throwIf(c == null, ErrorCode.NOT_FOUND_ERROR, "评论不存在");

        boolean isOwner = loginUser.getId().equals(c.getUserId());
        boolean isTargetAuthor = c.getTargetUserId() != null && loginUser.getId().equals(c.getTargetUserId());
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        ThrowUtils.throwIf(!(isOwner || isTargetAuthor || isAdmin), ErrorCode.NO_AUTH_ERROR);

        boolean isTopLevel = c.getParentCommentId() == null || c.getParentCommentId() == 0L;

        // 软删当前评论；如果是顶级评论则连同其子回复一起软删
        if (isTopLevel) {
            this.removeById(commentId);
            // 级联软删所有回复
            this.lambdaUpdate()
                    .eq(Comments::getRootCommentId, commentId)
                    .remove();
            // picture 计数递减
            if (TargetTypeEnum.PICTURE.getValue() == c.getTargetType()) {
                pictureService.lambdaUpdate()
                        .eq(Picture::getId, c.getTargetId())
                        .setSql("commentCount = GREATEST(0, IFNULL(commentCount,0) - 1)")
                        .update();
            }
        } else {
            this.removeById(commentId);
        }
        return true;
    }

    @Override
    public Page<CommentVO> listTopLevelComments(CommentQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long targetId = request.getTargetId();
        Integer targetType = request.getTargetType();
        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!TargetTypeEnum.isValid(targetType), ErrorCode.PARAMS_ERROR);

        long current = Math.max(1, request.getCurrent());
        long size = Math.min(COMMENT_MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));

        Page<Comments> page = this.lambdaQuery()
                .eq(Comments::getTargetId, targetId)
                .eq(Comments::getTargetType, targetType)
                .eq(Comments::getParentCommentId, 0L)
                .orderByDesc(Comments::getCreateTime)
                .page(new Page<>(current, size));

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        if (CollUtil.isEmpty(page.getRecords())) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        List<Comments> tops = page.getRecords();
        List<Long> rootIds = tops.stream().map(Comments::getCommentId).collect(Collectors.toList());

        // 子回复数
        Map<Long, Long> childCountMap = new HashMap<>();
        List<Map<String, Object>> cntRows = baseMapper.countChildrenByRoot(rootIds);
        for (Map<String, Object> r : cntRows) {
            Object rid = r.get("rootCommentId");
            Object cnt = r.get("cnt");
            if (rid != null && cnt != null) {
                childCountMap.put(((Number) rid).longValue(), ((Number) cnt).longValue());
            }
        }

        // 每个根评论前 N 条回复预览
        List<Comments> previewChildren = baseMapper.listChildPreviewByRoot(rootIds, CHILD_PREVIEW_COUNT);

        // 聚合所有需要的 userId
        Set<Long> userIds = new HashSet<>();
        tops.forEach(c -> userIds.add(c.getUserId()));
        previewChildren.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        });
        Map<Long, UserVO> userVOMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        // 将子评论按 root 分组
        Map<Long, List<CommentVO>> childVOByRoot = new HashMap<>();
        for (Comments child : previewChildren) {
            CommentVO cv = CommentVO.objToVo(child);
            cv.setUser(userVOMap.get(child.getUserId()));
            if (child.getReplyToUserId() != null) {
                cv.setReplyToUser(userVOMap.get(child.getReplyToUserId()));
            }
            childVOByRoot.computeIfAbsent(child.getRootCommentId(), k -> new ArrayList<>()).add(cv);
        }

        List<CommentVO> voList = new ArrayList<>(tops.size());
        for (Comments c : tops) {
            CommentVO vo = CommentVO.objToVo(c);
            vo.setUser(userVOMap.get(c.getUserId()));
            vo.setChildCount(childCountMap.getOrDefault(c.getCommentId(), 0L));
            vo.setChildComments(childVOByRoot.getOrDefault(c.getCommentId(), new ArrayList<>()));
            vo.setIsLiked(false);
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Page<CommentVO> listChildComments(Long rootCommentId, long current, long size, User loginUser) {
        ThrowUtils.throwIf(rootCommentId == null || rootCommentId <= 0, ErrorCode.PARAMS_ERROR);
        current = Math.max(1, current);
        size = Math.min(COMMENT_MAX_PAGE_SIZE, Math.max(1, size));

        QueryWrapper<Comments> qw = new QueryWrapper<>();
        qw.eq("rootCommentId", rootCommentId)
                .gt("parentCommentId", 0)
                .orderByAsc("createTime");
        Page<Comments> page = this.page(new Page<>(current, size), qw);

        Page<CommentVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Comments> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> userIds = new HashSet<>();
        records.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) userIds.add(c.getReplyToUserId());
        });
        Map<Long, UserVO> userVOMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        List<CommentVO> voList = records.stream().map(c -> {
            CommentVO vo = CommentVO.objToVo(c);
            vo.setUser(userVOMap.get(c.getUserId()));
            if (c.getReplyToUserId() != null) {
                vo.setReplyToUser(userVOMap.get(c.getReplyToUserId()));
            }
            vo.setIsLiked(false);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }
}
