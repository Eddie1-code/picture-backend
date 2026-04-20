package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.InteractionEvent;
import com.xcw.picturebackend.manager.social.InteractionLockManager;
import com.xcw.picturebackend.manager.social.InteractionStreamProducer;
import com.xcw.picturebackend.manager.social.UnreadRedisManager;
import com.xcw.picturebackend.mapper.LikeRecordMapper;
import com.xcw.picturebackend.model.dto.interaction.LikeQueryRequest;
import com.xcw.picturebackend.model.entity.LikeRecord;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Post;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.model.vo.LikeVO;
import com.xcw.picturebackend.model.vo.PictureVO;
import com.xcw.picturebackend.model.vo.PostVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.LikeRecordService;
import com.xcw.picturebackend.service.PictureService;
import com.xcw.picturebackend.service.PostService;
import com.xcw.picturebackend.service.SpaceService;
import com.xcw.picturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用点赞服务实现
 * - 同一用户对同一目标重复点击：2s Redis 锁拒绝 + DB 唯一键兜底
 * - 计数一致性：点赞 / 取消 与 targets 表 likeCount +/-1 在同一事务内
 */
@Slf4j
@Service
public class LikeRecordServiceImpl extends ServiceImpl<LikeRecordMapper, LikeRecord>
        implements LikeRecordService {

    @Resource
    private InteractionLockManager interactionLockManager;

    @Resource
    private UnreadRedisManager unreadRedisManager;

    @Resource
    private InteractionStreamProducer interactionStreamProducer;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Lazy
    @Resource
    private PostService postService;

    private static final int LIKE_MAX_PAGE_SIZE = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Long targetId, Integer targetType, User loginUser) {
        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR, "目标 id 非法");
        ThrowUtils.throwIf(!TargetTypeEnum.isValid(targetType), ErrorCode.PARAMS_ERROR, "目标类型非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = loginUser.getId();

        // 1) 短时重复点击保护
        if (!interactionLockManager.tryLockLike(userId, targetType, targetId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        // 2) 校验目标存在 + 找到目标所属用户 + 校验「允许点赞」
        Long targetUserId;
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            Picture p = pictureService.getById(targetId);
            ThrowUtils.throwIf(p == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (p.getAllowLike() != null && p.getAllowLike() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭点赞");
            }
            targetUserId = p.getUserId();
        } else if (TargetTypeEnum.POST.getValue() == targetType) {
            Post post = postService.getById(targetId);
            ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
            if (post.getAllowLike() != null && post.getAllowLike() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭点赞");
            }
            targetUserId = post.getUserId();
        } else if (TargetTypeEnum.SPACE.getValue() == targetType) {
            Space s = spaceService.getById(targetId);
            ThrowUtils.throwIf(s == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            targetUserId = s.getUserId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该目标类型");
        }

        // 3) 查询现状
        LikeRecord existed = this.lambdaQuery()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, targetId)
                .eq(LikeRecord::getTargetType, targetType)
                .one();

        int newLiked;
        int delta;
        if (existed == null) {
            newLiked = 1;
            delta = +1;
        } else if (existed.getIsLiked() != null && existed.getIsLiked() == 1) {
            newLiked = 0;
            delta = -1;
        } else {
            newLiked = 1;
            delta = +1;
        }

        // 4) upsert 点赞记录
        baseMapper.upsertLike(userId, targetId, targetType, targetUserId, newLiked);

        // 5) 计数字段同事务更新（GREATEST 避免并发下为负）
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            pictureService.lambdaUpdate()
                    .eq(Picture::getId, targetId)
                    .setSql("likeCount = GREATEST(0, IFNULL(likeCount,0) + (" + delta + "))")
                    .update();
        } else if (TargetTypeEnum.POST.getValue() == targetType) {
            postService.lambdaUpdate()
                    .eq(Post::getId, targetId)
                    .setSql("likeCount = GREATEST(0, IFNULL(likeCount,0) + (" + delta + "))")
                    .update();
        }
        // SPACE 的 likeCount 暂未加字段，如需请按同样方式扩展。

        // 只有「新增点赞」且目标作者非自己时，才在 Redis 未读 Hash 上累加
        if (newLiked == 1 && targetUserId != null && !targetUserId.equals(userId)) {
            unreadRedisManager.incLike(targetUserId);
        }

        // 发布互动事件到 Stream，供热榜 / 统计 / 未来推送消费
        interactionStreamProducer.publish(InteractionEvent.of(
                newLiked == 1 ? InteractionEvent.TYPE_LIKE : InteractionEvent.TYPE_UNLIKE,
                userId, targetUserId, targetType, targetId
        ));
        return newLiked == 1;
    }

    @Override
    public Set<Long> listLikedTargetIds(Long userId, Integer targetType, Collection<Long> targetIds) {
        if (userId == null || targetType == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptySet();
        }
        List<Long> list = baseMapper.listLikedTargetIds(userId, targetType, targetIds);
        return list == null ? Collections.emptySet() : new HashSet<>(list);
    }

    @Override
    public Page<LikeVO> listMyLikes(LikeQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long current = Math.max(1, request.getCurrent());
        long size = Math.min(LIKE_MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));

        Long targetUserId = request.getUserId() != null ? request.getUserId() : loginUser.getId();
        boolean isSelf = targetUserId.equals(loginUser.getId());
        if (!isSelf) {
            User owner = userService.getById(targetUserId);
            if (owner == null || (owner.getShowLikeList() != null && owner.getShowLikeList() == 0)) {
                Page<LikeVO> empty = new Page<>(current, size, 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
        }

        // 目前主要支持图片类点赞展示；帖子类（targetType=2）在帖子模块上线后再拓展
        Integer typeFilter = request.getTargetType();
        Page<LikeRecord> page = this.lambdaQuery()
                .eq(LikeRecord::getUserId, targetUserId)
                .eq(LikeRecord::getIsLiked, 1)
                .eq(typeFilter != null, LikeRecord::getTargetType, typeFilter)
                .orderByDesc(LikeRecord::getLastLikeTime)
                .page(new Page<>(current, size));

        Page<LikeVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<LikeRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> pictureIds = records.stream()
                .filter(r -> TargetTypeEnum.PICTURE.getValue() == r.getTargetType())
                .map(LikeRecord::getTargetId).collect(Collectors.toSet());
        Set<Long> postIds = records.stream()
                .filter(r -> TargetTypeEnum.POST.getValue() == r.getTargetType())
                .map(LikeRecord::getTargetId).collect(Collectors.toSet());

        Map<Long, Picture> pictureMap = pictureIds.isEmpty()
                ? Collections.emptyMap()
                : pictureService.listByIds(pictureIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p, (a, b) -> a));
        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postService.listByIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));

        Set<Long> authorIds = new HashSet<>();
        pictureMap.values().forEach(p -> {
            if (p.getUserId() != null) authorIds.add(p.getUserId());
        });
        postMap.values().forEach(p -> {
            if (p.getUserId() != null) authorIds.add(p.getUserId());
        });
        Map<Long, UserVO> authorVOMap = authorIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        List<LikeVO> voList = records.stream().map(r -> {
            LikeVO vo = new LikeVO();
            vo.setLikeRecordId(r.getId());
            vo.setTargetId(r.getTargetId());
            vo.setTargetType(r.getTargetType());
            vo.setLikeTime(r.getLastLikeTime() != null ? r.getLastLikeTime() : r.getFirstLikeTime());
            if (TargetTypeEnum.PICTURE.getValue() == r.getTargetType()) {
                Picture p = pictureMap.get(r.getTargetId());
                if (p != null) {
                    PictureVO pv = PictureVO.objToVo(p);
                    pv.setUser(authorVOMap.get(p.getUserId()));
                    pv.setIsLiked(true);
                    vo.setPicture(pv);
                }
            } else if (TargetTypeEnum.POST.getValue() == r.getTargetType()) {
                Post p = postMap.get(r.getTargetId());
                if (p != null) {
                    PostVO pv = PostVO.objToVo(p);
                    pv.setUser(authorVOMap.get(p.getUserId()));
                    pv.setIsLiked(true);
                    vo.setPost(pv);
                }
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public Map<Long, Long> countLikes(Integer targetType, Collection<Long> targetIds) {
        if (targetType == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = baseMapper.countLikes(targetType, targetIds);
        Map<Long, Long> result = new HashMap<>(rows.size() * 2);
        for (Map<String, Object> row : rows) {
            Object tid = row.get("targetId");
            Object cnt = row.get("cnt");
            if (tid != null && cnt != null) {
                result.put(((Number) tid).longValue(), ((Number) cnt).longValue());
            }
        }
        return result;
    }
}
