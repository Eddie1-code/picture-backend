package com.xcw.picturebackend.constant;

/**
 * 社交互动相关的 Redis Key 前缀约定。
 * 所有 Key 都以 "picture:social:" 开头，避免与业务缓存冲突。
 */
public final class SocialRedisKey {

    private SocialRedisKey() {}

    /** 统一前缀 */
    public static final String PREFIX = "picture:social:";

    /**
     * 点赞/收藏 防重复点击锁（SETNX, 2s 过期）
     * 结构：picture:social:lock:like:{userId}:{targetType}:{targetId}
     */
    public static String likeLockKey(Long userId, Integer targetType, Long targetId) {
        return PREFIX + "lock:like:" + userId + ":" + targetType + ":" + targetId;
    }

    public static String favoriteLockKey(Long userId, Integer targetType, Long targetId) {
        return PREFIX + "lock:fav:" + userId + ":" + targetType + ":" + targetId;
    }

    public static String commentLockKey(Long userId) {
        return PREFIX + "lock:comment:" + userId;
    }

    /**
     * 关注/取关 防重复锁
     * 结构：picture:social:lock:follow:{followerId}:{followingId}
     */
    public static String followLockKey(Long followerId, Long followingId) {
        return PREFIX + "lock:follow:" + followerId + ":" + followingId;
    }

    /**
     * 某用户的关注者集合（关注了谁）
     * 结构：picture:social:follow:following:{userId}  -> Set<Long>
     */
    public static String followingSetKey(Long userId) {
        return PREFIX + "follow:following:" + userId;
    }

    /**
     * 某用户的粉丝集合
     * 结构：picture:social:follow:fans:{userId}  -> Set<Long>
     */
    public static String fansSetKey(Long userId) {
        return PREFIX + "follow:fans:" + userId;
    }

    /**
     * 用户未读系统通知计数
     */
    public static String unreadSystemNotifyKey(Long userId) {
        return PREFIX + "unread:sysNotify:" + userId;
    }

    /**
     * 用户私聊某会话的未读计数（会话维度）
     */
    public static String unreadChatKey(Long userId) {
        return PREFIX + "unread:chat:" + userId;
    }

    /** 锁过期时间（毫秒） */
    public static final long LOCK_TTL_MS = 2000L;

    /** 关注关系缓存 TTL（秒） */
    public static final long FOLLOW_CACHE_TTL_SECONDS = 30L * 60L;
}
