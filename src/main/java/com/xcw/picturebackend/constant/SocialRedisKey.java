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

    /**
     * 发帖幂等锁：按 userId + clientReqId（前端生成）去重，TTL 10s
     */
    public static String postCreateLockKey(Long userId, String clientReqId) {
        return PREFIX + "lock:post:create:" + userId + ":" + clientReqId;
    }

    /**
     * 私信幂等锁：按 senderId + clientMsgId 去重，TTL 5min
     */
    public static String chatMsgLockKey(Long senderId, String clientMsgId) {
        return PREFIX + "lock:chat:msg:" + senderId + ":" + clientMsgId;
    }

    /**
     * 聚合未读 Hash：按业务类型维度存储
     * 结构：picture:social:unread:hash:{userId}  -> Hash<field, long>
     *   field ∈ { like, favorite, comment, follow, system, chat }
     */
    public static String unreadHashKey(Long userId) {
        return PREFIX + "unread:hash:" + userId;
    }

    /** 锁过期时间（毫秒） */
    public static final long LOCK_TTL_MS = 2000L;

    /** 关注关系缓存 TTL（秒） */
    public static final long FOLLOW_CACHE_TTL_SECONDS = 30L * 60L;

    /** 未读计数缓存 TTL（秒） */
    public static final long UNREAD_CACHE_TTL_SECONDS = 30L * 60L;

    /** 未读 Hash 字段名 */
    public static final String UNREAD_FIELD_LIKE = "like";
    public static final String UNREAD_FIELD_FAVORITE = "favorite";
    public static final String UNREAD_FIELD_COMMENT = "comment";
    public static final String UNREAD_FIELD_FOLLOW = "follow";
    public static final String UNREAD_FIELD_SYSTEM = "system";
    public static final String UNREAD_FIELD_CHAT = "chat";

    // ===================== Stream / ZSet / Online =====================

    /** 全局互动事件流（XADD 生产） */
    public static final String STREAM_INTERACTION = PREFIX + "stream:interaction";

    public static final String STREAM_GROUP_NOTIFY = "g-notify";
    public static final String STREAM_GROUP_HOTSCORE = "g-hotscore";
    public static final String STREAM_GROUP_STAT = "g-stat";

    /** 帖子热榜 ZSET：score = 热度分，member = postId */
    public static final String POST_HOT_RANK = PREFIX + "post:hotrank";

    /** 用户在线心跳（字符串 + TTL），TTL 过后视为离线 */
    public static String userOnlineKey(Long userId) {
        return PREFIX + "online:" + userId;
    }

    /** 会话列表缓存 ZSET：score = lastMessageTime，member = conversationId */
    public static String chatConvListKey(Long userId) {
        return PREFIX + "chat:convlist:" + userId;
    }

    public static final long USER_ONLINE_TTL_SECONDS = 120L;
}
