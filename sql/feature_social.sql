-- =====================================================================
-- 云图库·社交互动模块 一次性建表/改表脚本
-- 模块内容：点赞 / 收藏 / 评论 / 关注 / 私信 / 消息中心
-- 数据库：picture   字符集：utf8mb4   引擎：InnoDB
-- 约定：
--   targetType: 1-图片 2-帖子 3-空间   （帖子预留，当前只用 1/3）
--   isDelete:   0-未删  1-已删
--   isRead:     0-未读  1-已读
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1) user / picture 的社交字段已并入 picture.sql 最新结构
--    这里不再重复 ALTER，避免重复执行时报 Duplicate column/index 错误
-- ---------------------------------------------------------------------

-- =====================================================================
-- 第一期：点赞 / 收藏 / 评论
-- =====================================================================

-- ---------------------------------------------------------------------
-- like_record  通用点赞表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record`
(
    id             BIGINT     AUTO_INCREMENT COMMENT '主键 ID' PRIMARY KEY,
    userId         BIGINT                               NOT NULL COMMENT '点赞用户 ID',
    targetId       BIGINT                               NOT NULL COMMENT '被点赞内容 ID',
    targetType     TINYINT                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId   BIGINT                               NOT NULL COMMENT '被点赞内容所属用户 ID',
    isLiked        TINYINT(1) DEFAULT 1                 NOT NULL COMMENT '是否点赞：1-已赞 0-已取消',
    firstLikeTime  DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '首次点赞时间',
    lastLikeTime   DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '最近一次点赞时间',
    isRead         TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（红点消除用）',
    CONSTRAINT uk_user_target UNIQUE (userId, targetId, targetType)
) COMMENT = '通用点赞表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_like_target            ON like_record (targetId, targetType);
CREATE INDEX idx_like_targetUser_isRead ON like_record (targetUserId, isRead);
CREATE INDEX idx_like_user_type         ON like_record (userId, targetType);

-- ---------------------------------------------------------------------
-- favorite_record  通用收藏表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `favorite_record`;
CREATE TABLE `favorite_record`
(
    id           BIGINT     AUTO_INCREMENT COMMENT '主键 ID' PRIMARY KEY,
    userId       BIGINT                               NOT NULL COMMENT '收藏用户 ID',
    targetId     BIGINT                               NOT NULL COMMENT '被收藏内容 ID',
    targetType   TINYINT                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId BIGINT                               NOT NULL COMMENT '被收藏内容所属用户 ID',
    isFavorite   TINYINT(1) DEFAULT 1                 NOT NULL COMMENT '是否收藏：1-已收藏 0-已取消',
    favoriteTime DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '收藏时间',
    isRead       TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（红点消除用）',
    createTime   DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime   DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT    DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_user_target_favorite UNIQUE (userId, targetId, targetType)
) COMMENT = '通用收藏表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_fav_target            ON favorite_record (targetId, targetType);
CREATE INDEX idx_fav_targetUser_isRead ON favorite_record (targetUserId, isRead);
CREATE INDEX idx_fav_user_type         ON favorite_record (userId, targetType);

-- ---------------------------------------------------------------------
-- comments  通用评论表（两级：评论 + 回复；rootCommentId 便于未来无痛升级盖楼）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments`
(
    commentId       BIGINT     AUTO_INCREMENT COMMENT '评论 ID' PRIMARY KEY,
    userId          BIGINT                               NOT NULL COMMENT '评论用户 ID',
    targetId        BIGINT                               NOT NULL COMMENT '评论目标 ID',
    targetType      TINYINT    DEFAULT 1                 NOT NULL COMMENT '评论目标类型：1-图片 2-帖子 3-空间',
    targetUserId    BIGINT                               NOT NULL COMMENT '目标所属用户 ID（作者）',
    content         TEXT                                 NOT NULL COMMENT '评论内容',
    parentCommentId BIGINT     DEFAULT 0                     NULL COMMENT '父评论 ID（0=顶级评论）',
    rootCommentId   BIGINT                                   NULL COMMENT '根评论 ID（顶级评论 id，顶级行为空或等于 commentId）',
    replyToUserId   BIGINT                                   NULL COMMENT '被回复的用户 ID（显示 @xxx）',
    likeCount       BIGINT     DEFAULT 0                 NOT NULL COMMENT '评论点赞数',
    dislikeCount    BIGINT     DEFAULT 0                 NOT NULL COMMENT '评论点踩数',
    location        VARCHAR(128)                             NULL COMMENT '评论位置（省份）',
    isRead          TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（红点消除用）',
    createTime      DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME   DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT(1) DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT = '通用评论表（两级：评论+回复）' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_cmt_target_delete_time   ON comments (targetId, targetType, isDelete, createTime DESC);
CREATE INDEX idx_cmt_parent_delete_time   ON comments (parentCommentId, isDelete, createTime);
CREATE INDEX idx_cmt_root_delete_time     ON comments (rootCommentId, isDelete, createTime);
CREATE INDEX idx_cmt_targetUser_isRead    ON comments (targetUserId, isRead);
CREATE INDEX idx_cmt_user_delete_time     ON comments (userId, isDelete, createTime DESC);

-- =====================================================================
-- 第二期：关注 / 粉丝
-- =====================================================================

-- ---------------------------------------------------------------------
-- userfollows  用户关注关系表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `userfollows`;
CREATE TABLE `userfollows`
(
    followId            BIGINT   AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    followerId          BIGINT                             NOT NULL COMMENT '关注者（主动关注方）用户 ID',
    followingId         BIGINT                             NOT NULL COMMENT '被关注者用户 ID',
    followStatus        TINYINT  DEFAULT 1                 NOT NULL COMMENT '关注状态：1-关注中 0-已取消',
    isMutual            TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否双向关注：1-是 0-否',
    isRead              TINYINT(1) DEFAULT 0               NOT NULL COMMENT '被关注方是否已读该关注',
    lastInteractionTime DATETIME                               NULL COMMENT '最后交互时间',
    createTime          DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime          DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete            TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_follower_following UNIQUE (followerId, followingId)
) COMMENT = '用户关注关系表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_follower_status      ON userfollows (followerId, followStatus, isDelete);
CREATE INDEX idx_following_status     ON userfollows (followingId, followStatus, isDelete);
CREATE INDEX idx_following_isRead     ON userfollows (followingId, isRead);

-- =====================================================================
-- 第三期：私信
-- =====================================================================

-- ---------------------------------------------------------------------
-- private_chat  私聊会话表（一个会话一行，双方共用）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `private_chat`;
CREATE TABLE `private_chat`
(
    id                    BIGINT    AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    userId                BIGINT                              NOT NULL COMMENT '会话发起方用户 ID（小的一方，保证唯一会话）',
    targetUserId          BIGINT                              NOT NULL COMMENT '会话目标用户 ID（大的一方）',
    lastMessage           VARCHAR(512)                            NULL COMMENT '最后一条消息预览',
    lastMessageType       VARCHAR(16) DEFAULT 'text'              NULL COMMENT '最后一条消息类型：text/image',
    lastMessageTime       DATETIME                                NULL COMMENT '最后一条消息时间',
    userUnreadCount       INT       DEFAULT 0                     NULL COMMENT 'userId 方未读数',
    targetUserUnreadCount INT       DEFAULT 0                     NULL COMMENT 'targetUserId 方未读数',
    userChatName          VARCHAR(50)                             NULL COMMENT 'userId 自定义的对方备注名',
    targetUserChatName    VARCHAR(50)                             NULL COMMENT 'targetUserId 自定义的对方备注名',
    chatType              TINYINT   DEFAULT 0                 NOT NULL COMMENT '会话类型：0-陌生人私信 1-好友(双向关注)',
    createTime            DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime            DATETIME  DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete              TINYINT   DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_private_chat_pair UNIQUE (userId, targetUserId)
) COMMENT = '私聊会话表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_pc_user          ON private_chat (userId, isDelete, lastMessageTime DESC);
CREATE INDEX idx_pc_targetUser    ON private_chat (targetUserId, isDelete, lastMessageTime DESC);
CREATE INDEX idx_pc_type          ON private_chat (chatType);

-- ---------------------------------------------------------------------
-- chat_message  聊天消息明细表（私信 / 预留空间内聊天）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message`
(
    id              BIGINT      AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    privateChatId   BIGINT                                    NULL COMMENT '私聊会话 ID（ private_chat.id ）',
    senderId        BIGINT                                NOT NULL COMMENT '发送者 ID',
    receiverId      BIGINT                                    NULL COMMENT '接收者 ID（私聊必填）',
    spaceId         BIGINT                                    NULL COMMENT '空间 ID（空间内聊天预留，私聊为空）',
    pictureId       BIGINT                                    NULL COMMENT '关联图片 ID（分享图片时使用）',
    content         TEXT                                  NOT NULL COMMENT '消息文本内容',
    messageType     VARCHAR(16) DEFAULT 'text'            NOT NULL COMMENT '消息类型：text-文本 image-图片',
    messageUrl      VARCHAR(1024)                             NULL COMMENT '资源地址（图片 URL）',
    messageSize     BIGINT      DEFAULT 0                     NULL COMMENT '资源大小（字节）',
    messageLocation VARCHAR(256)                              NULL COMMENT '消息发送位置',
    replyId         BIGINT                                    NULL COMMENT '被引用回复的消息 ID',
    status          TINYINT     DEFAULT 0                 NOT NULL COMMENT '消息状态：0-未读 1-已读 2-已撤回',
    clientMsgId     VARCHAR(64)                               NULL COMMENT '客户端消息 ID（幂等去重）',
    createTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete        TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT = '聊天消息表（私信/预留空间聊天）' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_cm_privateChat_time ON chat_message (privateChatId, isDelete, createTime);
CREATE INDEX idx_cm_sender_receiver  ON chat_message (senderId, receiverId, createTime);
CREATE INDEX idx_cm_receiver_status  ON chat_message (receiverId, status, isDelete);
CREATE INDEX idx_cm_clientMsgId      ON chat_message (clientMsgId);

-- =====================================================================
-- 第四期：消息中心
--   设计思路：点赞/评论/收藏/关注 四类消息直接查对应业务表的 isRead
--             系统通知单独建表（管理员发公告、审核结果通知等）
-- =====================================================================

-- ---------------------------------------------------------------------
-- t_system_notify  系统通知表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `t_system_notify`;
CREATE TABLE `t_system_notify`
(
    id             BIGINT      AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    senderType     VARCHAR(20) DEFAULT 'SYSTEM'          NOT NULL COMMENT '发送者类型：ADMIN/SYSTEM',
    senderId       VARCHAR(50) DEFAULT 'system'          NOT NULL COMMENT '发送者 ID（ADMIN=管理员ID / SYSTEM=system）',
    receiverType   VARCHAR(20)                           NOT NULL COMMENT '接收者类型：ALL_USER/SPECIFIC_USER/ROLE',
    receiverId     VARCHAR(50)                               NULL COMMENT '接收者 ID：ALL=NULL / SPECIFIC=userId / ROLE=user|admin|vip',
    notifyType     VARCHAR(30)                           NOT NULL COMMENT '通知类型：ADMIN_ANNOUNCE / PICTURE_REVIEW / ACCOUNT / SYSTEM_ALERT',
    notifyIcon     VARCHAR(50) DEFAULT 'default'         NOT NULL COMMENT '通知图标标识',
    title          VARCHAR(100)                          NOT NULL COMMENT '通知标题',
    content        TEXT                                  NOT NULL COMMENT '通知内容（支持富文本）',
    relatedBizType VARCHAR(30)                               NULL COMMENT '关联业务类型：PICTURE/SPACE/COMMENT/NULL',
    relatedBizId   VARCHAR(50)                               NULL COMMENT '关联业务 ID（用于前端跳转）',
    isGlobal       TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否全局通知：1-是 0-否',
    expireTime     DATETIME                                  NULL COMMENT '过期时间（NULL=永久）',
    isEnabled      TINYINT     DEFAULT 1                 NOT NULL COMMENT '是否生效：1-生效 0-失效',
    createTime     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME    DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT     DEFAULT 0                 NOT NULL COMMENT '是否删除'
) COMMENT = '系统通知表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_sn_receiver    ON t_system_notify (receiverType, receiverId);
CREATE INDEX idx_sn_notify_type ON t_system_notify (notifyType);
CREATE INDEX idx_sn_create_time ON t_system_notify (createTime DESC);

-- ---------------------------------------------------------------------
-- user_system_notify_read  用户对系统通知的已读状态表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_system_notify_read`;
CREATE TABLE `user_system_notify_read`
(
    id             BIGINT   AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    userId         BIGINT                             NOT NULL COMMENT '用户 ID',
    systemNotifyId BIGINT                             NOT NULL COMMENT '系统通知 ID',
    readStatus     TINYINT  DEFAULT 0                 NOT NULL COMMENT '阅读状态：0-未读 1-已读',
    readTime       DATETIME                               NULL COMMENT '阅读时间',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete       TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_user_notify UNIQUE (userId, systemNotifyId)
) COMMENT = '用户系统通知阅读状态表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_usnr_user_read ON user_system_notify_read (userId, readStatus);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 完成
-- =====================================================================
