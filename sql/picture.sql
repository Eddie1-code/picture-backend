SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for picture
-- ----------------------------
DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片 url',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
  `introduction` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '简介',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '标签（JSON 数组）',
  `picSize` bigint DEFAULT NULL COMMENT '图片体积',
  `picWidth` int DEFAULT NULL COMMENT '图片宽度',
  `picHeight` int DEFAULT NULL COMMENT '图片高度',
  `picScale` double DEFAULT NULL COMMENT '图片宽高比例',
  `picFormat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片格式',
  `userId` bigint NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `reviewStatus` int NOT NULL DEFAULT '0' COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
  `reviewMessage` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核信息',
  `reviewerId` bigint DEFAULT NULL COMMENT '审核人 ID',
  `reviewTime` datetime DEFAULT NULL COMMENT '审核时间',
  `thumbnailUrl` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缩略图 url',
  `spaceId` bigint DEFAULT NULL COMMENT '空间 id（为空表示公共空间）',
  `picColor` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片主色调',
  `likeCount` bigint NOT NULL DEFAULT '0' COMMENT '点赞数（计数字段，以 like_record 为准）',
  `commentCount` bigint NOT NULL DEFAULT '0' COMMENT '评论数',
  `favoriteCount` bigint NOT NULL DEFAULT '0' COMMENT '收藏数',
  `viewCount` bigint NOT NULL DEFAULT '0' COMMENT '浏览量',
  `shareCount` bigint NOT NULL DEFAULT '0' COMMENT '分享数',
  `allowLike` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许点赞：1-允许 0-禁止',
  `allowComment` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许评论：1-允许 0-禁止',
  `allowCollect` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许收藏：1-允许 0-禁止',
  `allowShare` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许分享：1-允许 0-禁止',
  `hotScore` double NOT NULL DEFAULT '0' COMMENT '热榜分数（定时任务更新）',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_introduction` (`introduction`) USING BTREE,
  KEY `idx_category` (`category`) USING BTREE,
  KEY `idx_tags` (`tags`) USING BTREE,
  KEY `idx_userId` (`userId`) USING BTREE,
  KEY `idx_reviewStatus` (`reviewStatus`) USING BTREE,
  KEY `idx_spaceId` (`spaceId`) USING BTREE,
  KEY `idx_picture_hot_score` (`hotScore` DESC, `createTime` DESC) USING BTREE,
  KEY `idx_picture_like_count` (`likeCount` DESC, `createTime` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片' ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
  `vipExpireTime` datetime DEFAULT NULL COMMENT '会员过期时间',
  `vipCode` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '会员兑换码',
  `vipNumber` bigint DEFAULT NULL COMMENT '会员编号',
  `allowPrivateChat` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许被私聊：1-允许 0-禁止',
  `allowFollow` tinyint NOT NULL DEFAULT '1' COMMENT '是否允许被关注：1-允许 0-禁止',
  `showFollowList` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开关注列表：1-展示 0-隐藏',
  `showFansList` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开粉丝列表：1-展示 0-隐藏',
  `personalSign` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个性签名',
  `lastActiveTime` datetime DEFAULT NULL COMMENT '最后活跃时间',
  `showLikeList` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开「喜欢」列表：1-展示 0-隐藏',
  `showFavoriteList` tinyint NOT NULL DEFAULT '1' COMMENT '是否公开「收藏」列表：1-展示 0-隐藏',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_userAccount` (`userAccount`) USING BTREE,
  KEY `idx_userName` (`userName`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户' ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;


