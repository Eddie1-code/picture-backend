-- =====================================================================
-- 云图库 · 个人主页 Tab 化 隐私开关扩展脚本（幂等版）
-- 数据库：picture    字符集：utf8mb4   引擎：InnoDB
-- 依赖：feature_social.sql（已包含 allowPrivateChat / allowFollow / showFollowList / showFansList）
-- 说明：帖子模块 t_post 建表已拆分到 feature_post.sql
--       本脚本通过 information_schema 判断后再执行 ALTER，重复执行安全。
-- =====================================================================
SET NAMES utf8mb4;

-- 新增 showLikeList（若不存在）
SET @dbn := DATABASE();
SET @col := 'showLikeList';
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @dbn AND TABLE_NAME = 'user' AND COLUMN_NAME = @col);
SET @sql := IF(@exists = 0,
               'ALTER TABLE `user` ADD COLUMN showLikeList TINYINT DEFAULT 1 NOT NULL COMMENT ''是否公开「喜欢」列表：1-展示 0-隐藏''',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 新增 showFavoriteList（若不存在）
SET @col := 'showFavoriteList';
SET @exists := (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @dbn AND TABLE_NAME = 'user' AND COLUMN_NAME = @col);
SET @sql := IF(@exists = 0,
               'ALTER TABLE `user` ADD COLUMN showFavoriteList TINYINT DEFAULT 1 NOT NULL COMMENT ''是否公开「收藏」列表：1-展示 0-隐藏''',
               'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 完成
-- =====================================================================
