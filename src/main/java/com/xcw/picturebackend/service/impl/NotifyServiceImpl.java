package com.xcw.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.constant.SocialRedisKey;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.manager.social.UnreadRedisManager;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.mapper.CommentsMapper;
import com.xcw.picturebackend.mapper.FavoriteRecordMapper;
import com.xcw.picturebackend.mapper.LikeRecordMapper;
import com.xcw.picturebackend.mapper.SystemNotifyMapper;
import com.xcw.picturebackend.mapper.UserFollowMapper;
import com.xcw.picturebackend.mapper.UserSystemNotifyReadMapper;
import com.xcw.picturebackend.model.dto.notify.NotifyListRequest;
import com.xcw.picturebackend.model.entity.Comments;
import com.xcw.picturebackend.model.entity.FavoriteRecord;
import com.xcw.picturebackend.model.entity.LikeRecord;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.Space;
import com.xcw.picturebackend.model.entity.SystemNotify;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.entity.UserFollow;
import com.xcw.picturebackend.model.entity.UserSystemNotifyRead;
import com.xcw.picturebackend.model.enums.NotifyTypeEnum;
import com.xcw.picturebackend.model.enums.TargetTypeEnum;
import com.xcw.picturebackend.model.vo.NotifyItemVO;
import com.xcw.picturebackend.model.vo.NotifyUnreadVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.NotifyService;
import com.xcw.picturebackend.service.PictureService;
import com.xcw.picturebackend.service.SpaceService;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 消息中心统一服务：聚合点赞/评论/收藏/关注四类业务消息 + 系统通知。
 */
@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {

    private static final int NOTIFY_MAX_PAGE_SIZE = 50;

    @Resource
    private LikeRecordMapper likeRecordMapper;

    @Resource
    private FavoriteRecordMapper favoriteRecordMapper;

    @Resource
    private CommentsMapper commentsMapper;

    @Resource
    private UserFollowMapper userFollowMapper;

    @Resource
    private SystemNotifyMapper systemNotifyMapper;

    @Resource
    private UserSystemNotifyReadMapper userSystemNotifyReadMapper;

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UnreadRedisManager unreadRedisManager;

    @Override
    public NotifyUnreadVO getUnreadSummary(User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long uid = loginUser.getId();

        // 1) 优先从 Redis Hash 读取聚合未读
        Map<String, Long> cached = unreadRedisManager.getAll(uid);
        if (cached != null) {
            NotifyUnreadVO vo = buildVoFromMap(cached);
            return vo;
        }

        // 2) 缓存未命中：回源 DB 统计
        long likeCount = likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetUserId, uid)
                .eq(LikeRecord::getIsLiked, 1)
                .eq(LikeRecord::getIsRead, 0));

        long favCount = favoriteRecordMapper.selectCount(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getTargetUserId, uid)
                .eq(FavoriteRecord::getIsFavorite, 1)
                .eq(FavoriteRecord::getIsRead, 0));

        long commentCount = commentsMapper.selectCount(new LambdaQueryWrapper<Comments>()
                .and(w -> w.eq(Comments::getTargetUserId, uid).or().eq(Comments::getReplyToUserId, uid))
                .ne(Comments::getUserId, uid)
                .eq(Comments::getIsRead, 0)
                .eq(Comments::getIsDelete, 0));

        long followCount = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, uid)
                .eq(UserFollow::getFollowStatus, 1)
                .eq(UserFollow::getIsRead, 0)
                .eq(UserFollow::getIsDelete, 0));

        long systemCount = countSystemUnread(uid);

        NotifyUnreadVO vo = new NotifyUnreadVO();
        vo.setLikeCount(likeCount);
        vo.setCommentCount(commentCount);
        vo.setFavoriteCount(favCount);
        vo.setFollowCount(followCount);
        vo.setSystemCount(systemCount);
        vo.setTotalCount(likeCount + commentCount + favCount + followCount + systemCount);

        // 3) 写回 Redis（chat 由 ChatService 自行维护）
        Map<String, Long> fresh = new java.util.HashMap<>(6);
        fresh.put(SocialRedisKey.UNREAD_FIELD_LIKE, likeCount);
        fresh.put(SocialRedisKey.UNREAD_FIELD_FAVORITE, favCount);
        fresh.put(SocialRedisKey.UNREAD_FIELD_COMMENT, commentCount);
        fresh.put(SocialRedisKey.UNREAD_FIELD_FOLLOW, followCount);
        fresh.put(SocialRedisKey.UNREAD_FIELD_SYSTEM, systemCount);
        unreadRedisManager.putAll(uid, fresh);

        // 兼容老字段：保留系统未读单值 key（已有调用方可能用）
        try {
            stringRedisTemplate.opsForValue().set(
                    SocialRedisKey.unreadSystemNotifyKey(uid),
                    String.valueOf(systemCount),
                    60L, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        return vo;
    }

    private NotifyUnreadVO buildVoFromMap(Map<String, Long> cached) {
        long like = cached.getOrDefault(SocialRedisKey.UNREAD_FIELD_LIKE, 0L);
        long fav = cached.getOrDefault(SocialRedisKey.UNREAD_FIELD_FAVORITE, 0L);
        long comment = cached.getOrDefault(SocialRedisKey.UNREAD_FIELD_COMMENT, 0L);
        long follow = cached.getOrDefault(SocialRedisKey.UNREAD_FIELD_FOLLOW, 0L);
        long system = cached.getOrDefault(SocialRedisKey.UNREAD_FIELD_SYSTEM, 0L);
        NotifyUnreadVO vo = new NotifyUnreadVO();
        vo.setLikeCount(like);
        vo.setFavoriteCount(fav);
        vo.setCommentCount(comment);
        vo.setFollowCount(follow);
        vo.setSystemCount(system);
        // total 仅聚合站内消息（不含 chat），与旧版保持一致
        vo.setTotalCount(like + fav + comment + follow + system);
        return vo;
    }

    @Override
    public IPage<NotifyItemVO> listMessages(NotifyListRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        NotifyTypeEnum type = NotifyTypeEnum.from(request.getNotifyType());
        ThrowUtils.throwIf(type == null, ErrorCode.PARAMS_ERROR, "notifyType 非法");
        long current = Math.max(1, request.getCurrent());
        long size = Math.min(NOTIFY_MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));
        boolean onlyUnread = Boolean.TRUE.equals(request.getOnlyUnread());

        switch (type) {
            case LIKE:
                return listLikeMessages(loginUser.getId(), current, size, onlyUnread);
            case FAVORITE:
                return listFavoriteMessages(loginUser.getId(), current, size, onlyUnread);
            case COMMENT:
                return listCommentMessages(loginUser.getId(), current, size, onlyUnread);
            case FOLLOW:
                return listFollowMessages(loginUser.getId(), current, size, onlyUnread);
            case SYSTEM:
                return listSystemMessages(loginUser.getId(), current, size, onlyUnread);
            default:
                return emptyPage(current, size);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markAllRead(String notifyType, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        NotifyTypeEnum type = NotifyTypeEnum.from(notifyType);
        ThrowUtils.throwIf(type == null, ErrorCode.PARAMS_ERROR);
        Long uid = loginUser.getId();
        // 先按类型将 Redis Hash 对应字段清零，失效策略交由下次 getUnreadSummary 重建
        switch (type) {
            case LIKE:
                unreadRedisManager.clearField(uid, SocialRedisKey.UNREAD_FIELD_LIKE);
                break;
            case FAVORITE:
                unreadRedisManager.clearField(uid, SocialRedisKey.UNREAD_FIELD_FAVORITE);
                break;
            case COMMENT:
                unreadRedisManager.clearField(uid, SocialRedisKey.UNREAD_FIELD_COMMENT);
                break;
            case FOLLOW:
                unreadRedisManager.clearField(uid, SocialRedisKey.UNREAD_FIELD_FOLLOW);
                break;
            case SYSTEM:
                unreadRedisManager.clearField(uid, SocialRedisKey.UNREAD_FIELD_SYSTEM);
                break;
            default:
                break;
        }
        switch (type) {
            case LIKE:
                LikeRecord lr = new LikeRecord();
                lr.setIsRead(1);
                likeRecordMapper.update(lr, new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getTargetUserId, uid)
                        .eq(LikeRecord::getIsRead, 0));
                return true;
            case FAVORITE:
                FavoriteRecord fr = new FavoriteRecord();
                fr.setIsRead(1);
                favoriteRecordMapper.update(fr, new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getTargetUserId, uid)
                        .eq(FavoriteRecord::getIsRead, 0));
                return true;
            case COMMENT:
                Comments c = new Comments();
                c.setIsRead(1);
                commentsMapper.update(c, new LambdaQueryWrapper<Comments>()
                        .and(w -> w.eq(Comments::getTargetUserId, uid).or().eq(Comments::getReplyToUserId, uid))
                        .ne(Comments::getUserId, uid)
                        .eq(Comments::getIsRead, 0)
                        .eq(Comments::getIsDelete, 0));
                return true;
            case FOLLOW:
                UserFollow uf = new UserFollow();
                uf.setIsRead(1);
                userFollowMapper.update(uf, new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, uid)
                        .eq(UserFollow::getIsRead, 0));
                return true;
            case SYSTEM:
                return markAllSystemRead(uid);
            default:
                return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markOneRead(String notifyType, Long bizId, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(bizId == null || bizId <= 0, ErrorCode.PARAMS_ERROR);
        NotifyTypeEnum type = NotifyTypeEnum.from(notifyType);
        ThrowUtils.throwIf(type == null, ErrorCode.PARAMS_ERROR);
        Long uid = loginUser.getId();
        boolean affected = false;
        switch (type) {
            case LIKE:
                LikeRecord lr = new LikeRecord();
                lr.setIsRead(1);
                affected = likeRecordMapper.update(lr, new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getId, bizId)
                        .eq(LikeRecord::getTargetUserId, uid)) > 0;
                if (affected) unreadRedisManager.invalidate(uid);
                return affected;
            case FAVORITE:
                FavoriteRecord fr = new FavoriteRecord();
                fr.setIsRead(1);
                affected = favoriteRecordMapper.update(fr, new LambdaQueryWrapper<FavoriteRecord>()
                        .eq(FavoriteRecord::getId, bizId)
                        .eq(FavoriteRecord::getTargetUserId, uid)) > 0;
                if (affected) unreadRedisManager.invalidate(uid);
                return affected;
            case COMMENT:
                Comments c = new Comments();
                c.setIsRead(1);
                affected = commentsMapper.update(c, new LambdaQueryWrapper<Comments>()
                        .eq(Comments::getCommentId, bizId)
                        .and(w -> w.eq(Comments::getTargetUserId, uid).or().eq(Comments::getReplyToUserId, uid))) > 0;
                if (affected) unreadRedisManager.invalidate(uid);
                return affected;
            case FOLLOW:
                UserFollow uf = new UserFollow();
                uf.setIsRead(1);
                affected = userFollowMapper.update(uf, new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowId, bizId)
                        .eq(UserFollow::getFollowingId, uid)) > 0;
                if (affected) unreadRedisManager.invalidate(uid);
                return affected;
            case SYSTEM:
                affected = markSystemNotifyRead(uid, bizId);
                if (affected) unreadRedisManager.invalidate(uid);
                return affected;
            default:
                return false;
        }
    }

    // --------------------- 点赞消息 ---------------------
    private IPage<NotifyItemVO> listLikeMessages(Long uid, long current, long size, boolean onlyUnread) {
        Page<LikeRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<LikeRecord> qw = new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getTargetUserId, uid)
                .eq(LikeRecord::getIsLiked, 1)
                .orderByDesc(LikeRecord::getLastLikeTime);
        if (onlyUnread) qw.eq(LikeRecord::getIsRead, 0);
        likeRecordMapper.selectPage(page, qw);

        Page<NotifyItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<LikeRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Set<Long> fromIds = records.stream().map(LikeRecord::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> fromMap = loadUserVOMap(fromIds);
        TargetContext tc = loadTargetContext(records, LikeRecord::getTargetType, LikeRecord::getTargetId);

        List<NotifyItemVO> list = records.stream().map(r -> {
            NotifyItemVO vo = new NotifyItemVO();
            vo.setBizId(r.getId());
            vo.setNotifyType(NotifyTypeEnum.LIKE.getValue());
            vo.setIsRead(Integer.valueOf(1).equals(r.getIsRead()));
            vo.setNotifyTime(r.getLastLikeTime());
            vo.setFromUser(fromMap.get(r.getUserId()));
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            tc.fill(vo, r.getTargetType(), r.getTargetId());
            vo.setText("赞了你的" + targetTypeText(r.getTargetType()));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(list);
        return voPage;
    }

    // --------------------- 收藏消息 ---------------------
    private IPage<NotifyItemVO> listFavoriteMessages(Long uid, long current, long size, boolean onlyUnread) {
        Page<FavoriteRecord> page = new Page<>(current, size);
        LambdaQueryWrapper<FavoriteRecord> qw = new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getTargetUserId, uid)
                .eq(FavoriteRecord::getIsFavorite, 1)
                .orderByDesc(FavoriteRecord::getFavoriteTime);
        if (onlyUnread) qw.eq(FavoriteRecord::getIsRead, 0);
        favoriteRecordMapper.selectPage(page, qw);

        Page<NotifyItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<FavoriteRecord> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Set<Long> fromIds = records.stream().map(FavoriteRecord::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> fromMap = loadUserVOMap(fromIds);
        TargetContext tc = loadTargetContext(records, FavoriteRecord::getTargetType, FavoriteRecord::getTargetId);

        List<NotifyItemVO> list = records.stream().map(r -> {
            NotifyItemVO vo = new NotifyItemVO();
            vo.setBizId(r.getId());
            vo.setNotifyType(NotifyTypeEnum.FAVORITE.getValue());
            vo.setIsRead(Integer.valueOf(1).equals(r.getIsRead()));
            vo.setNotifyTime(r.getFavoriteTime());
            vo.setFromUser(fromMap.get(r.getUserId()));
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            tc.fill(vo, r.getTargetType(), r.getTargetId());
            vo.setText("收藏了你的" + targetTypeText(r.getTargetType()));
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(list);
        return voPage;
    }

    // --------------------- 评论消息 ---------------------
    private IPage<NotifyItemVO> listCommentMessages(Long uid, long current, long size, boolean onlyUnread) {
        Page<Comments> page = new Page<>(current, size);
        LambdaQueryWrapper<Comments> qw = new LambdaQueryWrapper<Comments>()
                .and(w -> w.eq(Comments::getTargetUserId, uid).or().eq(Comments::getReplyToUserId, uid))
                .ne(Comments::getUserId, uid)
                .eq(Comments::getIsDelete, 0)
                .orderByDesc(Comments::getCreateTime);
        if (onlyUnread) qw.eq(Comments::getIsRead, 0);
        commentsMapper.selectPage(page, qw);

        Page<NotifyItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Comments> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Set<Long> fromIds = records.stream().map(Comments::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> fromMap = loadUserVOMap(fromIds);
        TargetContext tc = loadTargetContext(records, Comments::getTargetType, Comments::getTargetId);

        List<NotifyItemVO> list = records.stream().map(r -> {
            NotifyItemVO vo = new NotifyItemVO();
            vo.setBizId(r.getCommentId());
            vo.setNotifyType(NotifyTypeEnum.COMMENT.getValue());
            vo.setIsRead(Integer.valueOf(1).equals(r.getIsRead()));
            vo.setNotifyTime(r.getCreateTime());
            vo.setFromUser(fromMap.get(r.getUserId()));
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            tc.fill(vo, r.getTargetType(), r.getTargetId());
            String snippet = r.getContent() == null ? "" :
                    (r.getContent().length() > 40 ? r.getContent().substring(0, 40) + "…" : r.getContent());
            boolean isReplyToMe = r.getReplyToUserId() != null && r.getReplyToUserId().equals(uid);
            String action = isReplyToMe ? "回复了你：" : "评论了你的" + targetTypeText(r.getTargetType()) + "：";
            vo.setText(action + snippet);
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(list);
        return voPage;
    }

    // --------------------- 关注消息 ---------------------
    private IPage<NotifyItemVO> listFollowMessages(Long uid, long current, long size, boolean onlyUnread) {
        Page<UserFollow> page = new Page<>(current, size);
        LambdaQueryWrapper<UserFollow> qw = new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, uid)
                .eq(UserFollow::getFollowStatus, 1)
                .eq(UserFollow::getIsDelete, 0)
                .orderByDesc(UserFollow::getCreateTime);
        if (onlyUnread) qw.eq(UserFollow::getIsRead, 0);
        userFollowMapper.selectPage(page, qw);

        Page<NotifyItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserFollow> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }
        Set<Long> fromIds = records.stream().map(UserFollow::getFollowerId).collect(Collectors.toSet());
        Map<Long, UserVO> fromMap = loadUserVOMap(fromIds);

        List<NotifyItemVO> list = records.stream().map(r -> {
            NotifyItemVO vo = new NotifyItemVO();
            vo.setBizId(r.getFollowId());
            vo.setNotifyType(NotifyTypeEnum.FOLLOW.getValue());
            vo.setIsRead(Integer.valueOf(1).equals(r.getIsRead()));
            vo.setNotifyTime(r.getCreateTime());
            vo.setFromUser(fromMap.get(r.getFollowerId()));
            vo.setText(Integer.valueOf(1).equals(r.getIsMutual()) ? "关注了你（互相关注）" : "关注了你");
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(list);
        return voPage;
    }

    // --------------------- 系统通知 ---------------------
    private IPage<NotifyItemVO> listSystemMessages(Long uid, long current, long size, boolean onlyUnread) {
        String uidStr = String.valueOf(uid);
        Page<SystemNotify> page = new Page<>(current, size);
        LambdaQueryWrapper<SystemNotify> qw = new LambdaQueryWrapper<SystemNotify>()
                .eq(SystemNotify::getIsEnabled, 1)
                .eq(SystemNotify::getIsDelete, 0)
                .and(w -> w
                        .and(x -> x.eq(SystemNotify::getReceiverType, "SPECIFIC_USER").eq(SystemNotify::getReceiverId, uidStr))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ALL_USER"))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ROLE").eq(SystemNotify::getReceiverId, "user")))
                .orderByDesc(SystemNotify::getCreateTime);
        systemNotifyMapper.selectPage(page, qw);

        Page<NotifyItemVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<SystemNotify> records = page.getRecords();
        if (CollUtil.isEmpty(records)) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> ids = records.stream().map(SystemNotify::getId).collect(Collectors.toSet());
        Map<Long, Boolean> readMap = userSystemNotifyReadMapper.selectList(new LambdaQueryWrapper<UserSystemNotifyRead>()
                        .eq(UserSystemNotifyRead::getUserId, uid)
                        .in(UserSystemNotifyRead::getSystemNotifyId, ids))
                .stream().collect(Collectors.toMap(
                        UserSystemNotifyRead::getSystemNotifyId,
                        r -> Integer.valueOf(1).equals(r.getReadStatus()),
                        (a, b) -> a));

        List<NotifyItemVO> list = records.stream().filter(r -> {
                    if (onlyUnread) {
                        return !Boolean.TRUE.equals(readMap.get(r.getId()));
                    }
                    return true;
                })
                .map(r -> {
                    NotifyItemVO vo = new NotifyItemVO();
                    vo.setBizId(r.getId());
                    vo.setNotifyType(NotifyTypeEnum.SYSTEM.getValue());
                    vo.setIsRead(Boolean.TRUE.equals(readMap.get(r.getId())));
                    vo.setNotifyTime(r.getCreateTime());
                    vo.setTitle(r.getTitle());
                    vo.setText(r.getContent());
                    vo.setIcon(r.getNotifyIcon());
                    vo.setRelatedBizType(r.getRelatedBizType());
                    vo.setRelatedBizId(r.getRelatedBizId());
                    return vo;
                }).collect(Collectors.toList());
        voPage.setRecords(list);
        return voPage;
    }

    private long countSystemUnread(Long uid) {
        String uidStr = String.valueOf(uid);
        // 先查所有对该用户可见、未过期、启用中的通知总数
        List<SystemNotify> all = systemNotifyMapper.selectList(new LambdaQueryWrapper<SystemNotify>()
                .select(SystemNotify::getId)
                .eq(SystemNotify::getIsEnabled, 1)
                .eq(SystemNotify::getIsDelete, 0)
                .and(w -> w
                        .and(x -> x.eq(SystemNotify::getReceiverType, "SPECIFIC_USER").eq(SystemNotify::getReceiverId, uidStr))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ALL_USER"))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ROLE").eq(SystemNotify::getReceiverId, "user"))));
        if (all.isEmpty()) return 0L;
        Set<Long> ids = all.stream().map(SystemNotify::getId).collect(Collectors.toSet());
        long readCount = userSystemNotifyReadMapper.selectCount(new LambdaQueryWrapper<UserSystemNotifyRead>()
                .eq(UserSystemNotifyRead::getUserId, uid)
                .eq(UserSystemNotifyRead::getReadStatus, 1)
                .in(UserSystemNotifyRead::getSystemNotifyId, ids));
        return Math.max(0L, (long) ids.size() - readCount);
    }

    private boolean markSystemNotifyRead(Long uid, Long systemNotifyId) {
        UserSystemNotifyRead record = userSystemNotifyReadMapper.selectOne(new LambdaQueryWrapper<UserSystemNotifyRead>()
                .eq(UserSystemNotifyRead::getUserId, uid)
                .eq(UserSystemNotifyRead::getSystemNotifyId, systemNotifyId)
                .last("LIMIT 1"));
        if (record == null) {
            UserSystemNotifyRead r = new UserSystemNotifyRead();
            r.setUserId(uid);
            r.setSystemNotifyId(systemNotifyId);
            r.setReadStatus(1);
            r.setReadTime(new Date());
            userSystemNotifyReadMapper.insert(r);
            return true;
        }
        if (Integer.valueOf(1).equals(record.getReadStatus())) {
            return true;
        }
        record.setReadStatus(1);
        record.setReadTime(new Date());
        return userSystemNotifyReadMapper.updateById(record) > 0;
    }

    private boolean markAllSystemRead(Long uid) {
        String uidStr = String.valueOf(uid);
        List<SystemNotify> all = systemNotifyMapper.selectList(new LambdaQueryWrapper<SystemNotify>()
                .select(SystemNotify::getId)
                .eq(SystemNotify::getIsEnabled, 1)
                .eq(SystemNotify::getIsDelete, 0)
                .and(w -> w
                        .and(x -> x.eq(SystemNotify::getReceiverType, "SPECIFIC_USER").eq(SystemNotify::getReceiverId, uidStr))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ALL_USER"))
                        .or(x -> x.eq(SystemNotify::getReceiverType, "ROLE").eq(SystemNotify::getReceiverId, "user"))));
        for (SystemNotify n : all) {
            markSystemNotifyRead(uid, n.getId());
        }
        return true;
    }

    // --------------------- 工具 ---------------------
    private Map<Long, UserVO> loadUserVOMap(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) return Collections.emptyMap();
        return userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO, (a, b) -> a));
    }

    /**
     * 加载 records 里引用的图片/空间目标信息
     */
    private <T> TargetContext loadTargetContext(List<T> records,
                                                java.util.function.Function<T, Integer> typeFn,
                                                java.util.function.Function<T, Long> idFn) {
        Set<Long> picIds = new HashSet<>();
        Set<Long> spaceIds = new HashSet<>();
        for (T r : records) {
            Integer tt = typeFn.apply(r);
            Long tid = idFn.apply(r);
            if (tid == null || tt == null) continue;
            if (tt == TargetTypeEnum.PICTURE.getValue()) picIds.add(tid);
            else if (tt == TargetTypeEnum.SPACE.getValue()) spaceIds.add(tid);
        }
        Map<Long, Picture> pictureMap = picIds.isEmpty() ? Collections.emptyMap()
                : pictureService.listByIds(picIds).stream().collect(Collectors.toMap(Picture::getId, p -> p, (a, b) -> a));
        Map<Long, Space> spaceMap = spaceIds.isEmpty() ? Collections.emptyMap()
                : spaceService.listByIds(spaceIds).stream().collect(Collectors.toMap(Space::getId, s -> s, (a, b) -> a));
        return new TargetContext(pictureMap, spaceMap);
    }

    private String targetTypeText(Integer targetType) {
        if (targetType == null) return "内容";
        if (targetType == TargetTypeEnum.PICTURE.getValue()) return "图片";
        if (targetType == TargetTypeEnum.SPACE.getValue()) return "空间";
        return "内容";
    }

    private Page<NotifyItemVO> emptyPage(long current, long size) {
        Page<NotifyItemVO> p = new Page<>(current, size, 0);
        p.setRecords(Collections.emptyList());
        return p;
    }

    /**
     * 图片/空间目标信息上下文
     */
    private static class TargetContext {
        private final Map<Long, Picture> pictureMap;
        private final Map<Long, Space> spaceMap;

        TargetContext(Map<Long, Picture> pictureMap, Map<Long, Space> spaceMap) {
            this.pictureMap = pictureMap;
            this.spaceMap = spaceMap;
        }

        void fill(NotifyItemVO vo, Integer targetType, Long targetId) {
            if (targetType == null || targetId == null) return;
            if (targetType == TargetTypeEnum.PICTURE.getValue()) {
                Picture p = pictureMap.get(targetId);
                if (p != null) {
                    vo.setTargetTitle(StrUtil.blankToDefault(p.getName(), "图片"));
                    vo.setTargetCover(StrUtil.blankToDefault(p.getThumbnailUrl(), p.getUrl()));
                }
            } else if (targetType == TargetTypeEnum.SPACE.getValue()) {
                Space s = spaceMap.get(targetId);
                if (s != null) {
                    vo.setTargetTitle(StrUtil.blankToDefault(s.getSpaceName(), "空间"));
                }
            }
        }
    }
}
