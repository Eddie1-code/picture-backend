package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.InteractionEvent;
import com.xcw.picturebackend.manager.social.InteractionLockManager;
import com.xcw.picturebackend.manager.social.InteractionStreamProducer;
import com.xcw.picturebackend.manager.social.UnreadRedisManager;
import com.xcw.picturebackend.mapper.FavoriteRecordMapper;
import com.xcw.picturebackend.model.dto.interaction.FavoriteQueryRequest;
import com.xcw.picturebackend.model.entity.FavoriteRecord;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Post;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.model.vo.FavoriteVO;
import com.xcw.picturebackend.model.vo.PictureVO;
import com.xcw.picturebackend.model.vo.PostVO;
import com.xcw.picturebackend.model.vo.SpaceVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.FavoriteRecordService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用收藏服务实现
 */
@Slf4j
@Service
public class FavoriteRecordServiceImpl extends ServiceImpl<FavoriteRecordMapper, FavoriteRecord>
        implements FavoriteRecordService {

    private static final int FAVORITE_MAX_PAGE_SIZE = 30;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Long targetId, Integer targetType, User loginUser) {
        ThrowUtils.throwIf(targetId == null || targetId <= 0, ErrorCode.PARAMS_ERROR, "目标 id 非法");
        ThrowUtils.throwIf(!TargetTypeEnum.isValid(targetType), ErrorCode.PARAMS_ERROR, "目标类型非法");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long userId = loginUser.getId();

        if (!interactionLockManager.tryLockFavorite(userId, targetType, targetId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        Long targetUserId;
        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            Picture p = pictureService.getById(targetId);
            ThrowUtils.throwIf(p == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            if (p.getAllowCollect() != null && p.getAllowCollect() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭收藏");
            }
            targetUserId = p.getUserId();
        } else if (TargetTypeEnum.POST.getValue() == targetType) {
            Post post = postService.getById(targetId);
            ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
            if (post.getAllowCollect() != null && post.getAllowCollect() == 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "作者已关闭收藏");
            }
            targetUserId = post.getUserId();
        } else if (TargetTypeEnum.SPACE.getValue() == targetType) {
            Space s = spaceService.getById(targetId);
            ThrowUtils.throwIf(s == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            targetUserId = s.getUserId();
        } else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "暂不支持该目标类型");
        }

        FavoriteRecord existed = this.lambdaQuery()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getTargetId, targetId)
                .eq(FavoriteRecord::getTargetType, targetType)
                .one();

        int newFavorite;
        int delta;
        if (existed == null) {
            newFavorite = 1;
            delta = +1;
        } else if (existed.getIsFavorite() != null && existed.getIsFavorite() == 1) {
            newFavorite = 0;
            delta = -1;
        } else {
            newFavorite = 1;
            delta = +1;
        }

        baseMapper.upsertFavorite(userId, targetId, targetType, targetUserId, newFavorite);

        if (TargetTypeEnum.PICTURE.getValue() == targetType) {
            pictureService.lambdaUpdate()
                    .eq(Picture::getId, targetId)
                    .setSql("favoriteCount = GREATEST(0, IFNULL(favoriteCount,0) + (" + delta + "))")
                    .update();
        } else if (TargetTypeEnum.POST.getValue() == targetType) {
            postService.lambdaUpdate()
                    .eq(Post::getId, targetId)
                    .setSql("favoriteCount = GREATEST(0, IFNULL(favoriteCount,0) + (" + delta + "))")
                    .update();
        }

        if (newFavorite == 1 && targetUserId != null && !targetUserId.equals(userId)) {
            unreadRedisManager.incFavorite(targetUserId);
        }
        interactionStreamProducer.publish(InteractionEvent.of(
                newFavorite == 1 ? InteractionEvent.TYPE_FAVORITE : InteractionEvent.TYPE_UNFAVORITE,
                userId, targetUserId, targetType, targetId
        ));
        return newFavorite == 1;
    }

    @Override
    public Set<Long> listFavoriteTargetIds(Long userId, Integer targetType, Collection<Long> targetIds) {
        if (userId == null || targetType == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptySet();
        }
        List<Long> list = baseMapper.listFavoriteTargetIds(userId, targetType, targetIds);
        return list == null ? Collections.emptySet() : new HashSet<>(list);
    }

    @Override
    public Page<FavoriteVO> listMyFavorites(FavoriteQueryRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        long current = Math.max(1, request.getCurrent());
        long size = Math.min(FAVORITE_MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));
        Integer targetType = request.getTargetType();

        // 目标用户：默认本人；他人时校验隐私
        Long targetUserId = request.getUserId() != null ? request.getUserId() : loginUser.getId();
        boolean isSelf = targetUserId.equals(loginUser.getId());
        if (!isSelf) {
            User owner = userService.getById(targetUserId);
            if (owner == null || (owner.getShowFavoriteList() != null && owner.getShowFavoriteList() == 0)) {
                Page<FavoriteVO> empty = new Page<>(current, size, 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
        }

        Page<FavoriteRecord> page = this.lambdaQuery()
                .eq(FavoriteRecord::getUserId, targetUserId)
                .eq(FavoriteRecord::getIsFavorite, 1)
                .eq(targetType != null, FavoriteRecord::getTargetType, targetType)
                .orderByDesc(FavoriteRecord::getFavoriteTime)
                .page(new Page<>(current, size));

        Page<FavoriteVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<FavoriteRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        // 分组批查：图片 / 空间 / 帖子
        Set<Long> pictureIds = records.stream()
                .filter(r -> TargetTypeEnum.PICTURE.getValue() == r.getTargetType())
                .map(FavoriteRecord::getTargetId).collect(Collectors.toSet());
        Set<Long> spaceIds = records.stream()
                .filter(r -> TargetTypeEnum.SPACE.getValue() == r.getTargetType())
                .map(FavoriteRecord::getTargetId).collect(Collectors.toSet());
        Set<Long> postIds = records.stream()
                .filter(r -> TargetTypeEnum.POST.getValue() == r.getTargetType())
                .map(FavoriteRecord::getTargetId).collect(Collectors.toSet());

        Map<Long, Picture> pictureMap = pictureIds.isEmpty()
                ? Collections.emptyMap()
                : pictureService.listByIds(pictureIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p, (a, b) -> a));

        Map<Long, Space> spaceMap = spaceIds.isEmpty()
                ? Collections.emptyMap()
                : spaceService.listByIds(spaceIds).stream()
                .collect(Collectors.toMap(Space::getId, s -> s, (a, b) -> a));

        Map<Long, Post> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : postService.listByIds(postIds).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a));

        // 聚合作者
        Set<Long> authorIds = new HashSet<>();
        pictureMap.values().forEach(p -> {
            if (p.getUserId() != null) authorIds.add(p.getUserId());
        });
        spaceMap.values().forEach(s -> {
            if (s.getUserId() != null) authorIds.add(s.getUserId());
        });
        postMap.values().forEach(p -> {
            if (p.getUserId() != null) authorIds.add(p.getUserId());
        });
        Map<Long, UserVO> authorVOMap = authorIds.isEmpty()
                ? Collections.emptyMap()
                : userService.listByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));

        List<FavoriteVO> voList = records.stream().map(r -> {
            FavoriteVO vo = new FavoriteVO();
            vo.setFavoriteRecordId(r.getId());
            vo.setTargetId(r.getTargetId());
            vo.setTargetType(r.getTargetType());
            vo.setFavoriteTime(r.getFavoriteTime());
            if (TargetTypeEnum.PICTURE.getValue() == r.getTargetType()) {
                Picture p = pictureMap.get(r.getTargetId());
                if (p != null) {
                    PictureVO pv = PictureVO.objToVo(p);
                    pv.setUser(authorVOMap.get(p.getUserId()));
                    pv.setIsFavorite(true);
                    vo.setPicture(pv);
                }
            } else if (TargetTypeEnum.SPACE.getValue() == r.getTargetType()) {
                Space s = spaceMap.get(r.getTargetId());
                if (s != null) {
                    SpaceVO sv = SpaceVO.objToVo(s);
                    sv.setUser(authorVOMap.get(s.getUserId()));
                    vo.setSpace(sv);
                }
            } else if (TargetTypeEnum.POST.getValue() == r.getTargetType()) {
                Post p = postMap.get(r.getTargetId());
                if (p != null) {
                    PostVO pv = PostVO.objToVo(p);
                    pv.setUser(authorVOMap.get(p.getUserId()));
                    pv.setIsFavorite(true);
                    vo.setPost(pv);
                }
            }
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }
}
