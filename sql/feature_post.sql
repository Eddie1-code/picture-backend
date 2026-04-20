-- =====================================================================
-- 云图库 · 帖子/动态模块建表脚本
-- 数据库：picture    字符集：utf8mb4   引擎：InnoDB
-- 依赖：feature_social.sql（user 扩展字段 + 互动相关表）
-- 说明：
--   - imageUrls 存 1~9 张图的 CDN url（JSON 数组），不落 picture 表，与作品解耦
--   - 复用 like_record / favorite_record / comments 的 targetType=2
--   - reviewStatus 与 picture 一致，便于复用审核通道
--   - 全局按 createTime DESC 作为拉式 timeline 排序键，复合索引保证分页
-- 本脚本可幂等执行（MySQL 5.7 / 8.x 均兼容，索引通过 information_schema 判断）
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS t_post (
    id            BIGINT       AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    userId        BIGINT                                 NOT NULL COMMENT '作者',
    content       TEXT                                       NULL COMMENT '正文（纯文本，建议 1000 字以内）',
    imageUrls     JSON                                       NULL COMMENT '配图 url 数组，最多 9 张',
    tags          VARCHAR(512)                               NULL COMMENT '标签 JSON 数组',
    location      VARCHAR(128)                               NULL COMMENT '地理位置（省份）',
    visibility    TINYINT      DEFAULT 0                   NOT NULL COMMENT '0-公开 1-仅粉丝 2-仅自己',
    allowComment  TINYINT      DEFAULT 1                   NOT NULL COMMENT '是否允许评论',
    allowLike     TINYINT      DEFAULT 1                   NOT NULL COMMENT '是否允许点赞',
    allowCollect  TINYINT      DEFAULT 1                   NOT NULL COMMENT '是否允许收藏',
    likeCount     BIGINT       DEFAULT 0                   NOT NULL COMMENT '点赞数（冗余，以 like_record 为准）',
    commentCount  BIGINT       DEFAULT 0                   NOT NULL COMMENT '评论数',
    favoriteCount BIGINT       DEFAULT 0                   NOT NULL COMMENT '收藏数',
    viewCount     BIGINT       DEFAULT 0                   NOT NULL COMMENT '浏览量',
    shareCount    BIGINT       DEFAULT 0                   NOT NULL COMMENT '分享数',
    hotScore      DOUBLE       DEFAULT 0                   NOT NULL COMMENT '热榜分（定时任务刷新）',
    reviewStatus  TINYINT      DEFAULT 0                   NOT NULL COMMENT '0-待审核 1-通过 2-拒绝',
    reviewMessage VARCHAR(512)                               NULL COMMENT '审核意见',
    reviewerId    BIGINT                                     NULL COMMENT '审核人',
    reviewTime    DATETIME                                   NULL COMMENT '审核时间',
    status        TINYINT      DEFAULT 1                   NOT NULL COMMENT '0-草稿 1-已发布 2-已下架',
    createTime    DATETIME     DEFAULT CURRENT_TIMESTAMP   NOT NULL COMMENT '创建时间',
    updateTime    DATETIME     DEFAULT CURRENT_TIMESTAMP   NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT      DEFAULT 0                   NOT NULL COMMENT '是否删除'
) COMMENT = '帖子/动态表' COLLATE = utf8mb4_unicode_ci;

-- 幂等创建索引（MySQL 5.7 / 8.x 通用）
SET @dbn := DATABASE();

SET @idx := 'idx_post_user_time';
SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @dbn AND TABLE_NAME = 't_post' AND INDEX_NAME = @idx);
SET @sql := IF(@exists = 0,
               'CREATE INDEX idx_post_user_time ON t_post (userId, isDelete, createTime)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_post_hot';
SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @dbn AND TABLE_NAME = 't_post' AND INDEX_NAME = @idx);
SET @sql := IF(@exists = 0,
               'CREATE INDEX idx_post_hot ON t_post (isDelete, reviewStatus, hotScore, createTime)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx := 'idx_post_time';
SET @exists := (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @dbn AND TABLE_NAME = 't_post' AND INDEX_NAME = @idx);
SET @sql := IF(@exists = 0,
               'CREATE INDEX idx_post_time ON t_post (isDelete, reviewStatus, createTime)',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
-- =====================================================================
-- 完成
-- =====================================================================
