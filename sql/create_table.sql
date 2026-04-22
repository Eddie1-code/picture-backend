SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 用户表（仅结构）
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
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_userAccount` (`userAccount`) USING BTREE,
    KEY `idx_userName` (`userName`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- 空间成员表（仅结构）
DROP TABLE IF EXISTS `space_user`;
CREATE TABLE `space_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `spaceId` bigint NOT NULL COMMENT '空间 id',
    `userId` bigint NOT NULL COMMENT '用户 id',
    `spaceRole` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT 'viewer' COMMENT '空间角色：viewer/editor/admin',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_spaceId_userId` (`spaceId`, `userId`) USING BTREE,
    KEY `idx_spaceId` (`spaceId`) USING BTREE,
    KEY `idx_userId` (`userId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间用户关联';

SET FOREIGN_KEY_CHECKS = 1;

