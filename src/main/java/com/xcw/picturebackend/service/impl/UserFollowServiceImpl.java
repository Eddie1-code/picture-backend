package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.social.InteractionLockManager;
import com.xcw.picturebackend.mapper.UserFollowMapper;
import com.xcw.picturebackend.model.dto.userfollow.FollowActionRequest;
import com.xcw.picturebackend.model.dto.userfollow.FollowListRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.entity.UserFollow;
import com.xcw.picturebackend.model.vo.UserFollowVO;
import com.xcw.picturebackend.service.UserFollowService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户关注关系服务实现
 */
@Slf4j
@Service
public class UserFollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow>
        implements UserFollowService {

    private static final int FOLLOW_MAX_PAGE_SIZE = 50;

    @Resource
    private InteractionLockManager interactionLockManager;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFollow(FollowActionRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long followerId = loginUser.getId();
        Long followingId = request.getTargetUserId();
        ThrowUtils.throwIf(followingId == null || followingId <= 0, ErrorCode.PARAMS_ERROR, "目标用户非法");
        ThrowUtils.throwIf(followerId.equals(followingId), ErrorCode.PARAMS_ERROR, "不能关注自己");

        boolean follow = Boolean.TRUE.equals(request.getFollow());

        if (!interactionLockManager.tryLockFollow(followerId, followingId)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "操作过于频繁，请稍后再试");
        }

        User target = userService.getById(followingId);
        ThrowUtils.throwIf(target == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        if (follow && target.getAllowFollow() != null && target.getAllowFollow() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "对方未开放关注");
        }

        int status = follow ? 1 : 0;
        baseMapper.upsertFollow(followerId, followingId, status);

        // 维护双向关注标记
        UserFollow reverse = this.lambdaQuery()
                .eq(UserFollow::getFollowerId, followingId)
                .eq(UserFollow::getFollowingId, followerId)
                .eq(UserFollow::getFollowStatus, 1)
                .eq(UserFollow::getIsDelete, 0)
                .one();
        int mutual = (follow && reverse != null) ? 1 : 0;
        baseMapper.updateMutualFlag(followerId, followingId, mutual);

        // 缓存维护
        invalidateFollowCache(followerId, followingId);
        return follow;
    }

    private void invalidateFollowCache(Long followerId, Long followingId) {
        try {
            stringRedisTemplate.delete(SocialRedisKey.followingSetKey(followerId));
            stringRedisTemplate.delete(SocialRedisKey.fansSetKey(followingId));
        } catch (Exception e) {
            log.warn("invalidate follow cache failed, followerId={}, followingId={}", followerId, followingId, e);
        }
    }

    @Override
    public IPage<UserFollowVO> listFollowOrFans(FollowListRequest request, User loginUser) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Long targetUserId = request.getUserId() != null
                ? request.getUserId()
                : (loginUser != null ? loginUser.getId() : null);
        ThrowUtils.throwIf(targetUserId == null, ErrorCode.PARAMS_ERROR, "未指定用户");
        String type = StrUtil.isBlank(request.getType()) ? "following" : request.getType();

        long current = Math.max(1, request.getCurrent());
        long size = Math.min(FOLLOW_MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));

        Page<UserFollow> page = new Page<>(current, size);
        boolean isFollowing = "following".equalsIgnoreCase(type);
        this.lambdaQuery()
                .eq(isFollowing ? UserFollow::getFollowerId : UserFollow::getFollowingId, targetUserId)
                .eq(UserFollow::getFollowStatus, 1)
                .eq(UserFollow::getIsDelete, 0)
                .orderByDesc(UserFollow::getCreateTime)
                .page(page);

        Page<UserFollowVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserFollow> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> otherIds = records.stream()
                .map(r -> isFollowing ? r.getFollowingId() : r.getFollowerId())
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userService.listByIds(otherIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        Map<Long, Boolean> mineFollowMap;
        if (loginUser == null) {
            mineFollowMap = Collections.emptyMap();
        } else {
            mineFollowMap = batchIsFollowing(loginUser.getId(), new java.util.ArrayList<>(otherIds));
        }

        List<UserFollowVO> voList = records.stream().map(r -> {
            Long otherId = isFollowing ? r.getFollowingId() : r.getFollowerId();
            User u = userMap.get(otherId);
            UserFollowVO vo = new UserFollowVO();
            vo.setUserId(otherId);
            if (u != null) {
                vo.setUserName(u.getUserName());
                vo.setUserAvatar(u.getUserAvatar());
                vo.setUserProfile(u.getUserProfile());
                vo.setPersonalSign(u.getPersonalSign());
                vo.setUserRole(u.getUserRole());
            }
            vo.setFollowTime(r.getCreateTime());
            vo.setIsFollowed(mineFollowMap.getOrDefault(otherId, false));
            vo.setIsMutual(Integer.valueOf(1).equals(r.getIsMutual()));
            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null || followerId.equals(followingId)) {
            return false;
        }
        Set<Long> set = getFollowingIds(followerId);
        return set.contains(followingId);
    }

    @Override
    public Map<Long, Boolean> batchIsFollowing(Long followerId, List<Long> targetIds) {
        if (followerId == null || CollUtil.isEmpty(targetIds)) {
            return Collections.emptyMap();
        }
        Set<Long> followingSet = getFollowingIds(followerId);
        Map<Long, Boolean> map = new HashMap<>();
        for (Long id : targetIds) {
            if (id == null) continue;
            map.put(id, followingSet.contains(id));
        }
        return map;
    }

    @Override
    public long countFollowing(Long userId) {
        if (userId == null) return 0;
        return getFollowingIds(userId).size();
    }

    @Override
    public long countFans(Long userId) {
        if (userId == null) return 0;
        return getFansIds(userId).size();
    }

    @Override
    public Set<Long> getFollowingIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        String key = SocialRedisKey.followingSetKey(userId);
        try {
            Set<String> cached = stringRedisTemplate.opsForSet().members(key);
            if (cached != null && !cached.isEmpty()) {
                return toLongSet(cached);
            }
        } catch (Exception e) {
            log.warn("read following cache failed, userId={}", userId, e);
        }
        List<Long> ids = baseMapper.listFollowingIds(userId);
        Set<Long> result = ids == null ? Collections.emptySet() : new HashSet<>(ids);
        writeSetCache(key, result);
        return result;
    }

    @Override
    public Set<Long> getFansIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        String key = SocialRedisKey.fansSetKey(userId);
        try {
            Set<String> cached = stringRedisTemplate.opsForSet().members(key);
            if (cached != null && !cached.isEmpty()) {
                return toLongSet(cached);
            }
        } catch (Exception e) {
            log.warn("read fans cache failed, userId={}", userId, e);
        }
        List<Long> ids = baseMapper.listFollowerIds(userId);
        Set<Long> result = ids == null ? Collections.emptySet() : new HashSet<>(ids);
        writeSetCache(key, result);
        return result;
    }

    private void writeSetCache(String key, Set<Long> set) {
        try {
            if (set.isEmpty()) {
                // 占位 -1 防缓存穿透，TTL 短些
                stringRedisTemplate.opsForSet().add(key, "-1");
                stringRedisTemplate.expire(key, 60L, TimeUnit.SECONDS);
                return;
            }
            String[] members = set.stream().map(String::valueOf).toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(key, members);
            stringRedisTemplate.expire(key, SocialRedisKey.FOLLOW_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("write follow cache failed, key={}", key, e);
        }
    }

    private Set<Long> toLongSet(Set<String> src) {
        Set<Long> result = new HashSet<>(src.size());
        for (String s : src) {
            if (s == null || "-1".equals(s)) continue;
            try {
                result.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}
