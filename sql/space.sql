SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 空间表（仅结构）
DROP TABLE IF EXISTS `space`;
CREATE TABLE `space` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `spaceName` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '空间名称',
    `spaceLevel` int DEFAULT '0' COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    `maxSize` bigint DEFAULT '0' COMMENT '空间图片的最大总大小',
    `maxCount` bigint DEFAULT '0' COMMENT '空间图片的最大数量',
    `totalSize` bigint DEFAULT '0' COMMENT '当前空间下图片的总大小',
    `totalCount` bigint DEFAULT '0' COMMENT '当前空间下的图片数量',
    `userId` bigint NOT NULL COMMENT '创建用户 id',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除',
    `spaceType` int NOT NULL DEFAULT '0' COMMENT '空间类型：0-私有 1-团队',
    PRIMARY KEY (`id`) USING BTREE,
    KEY `idx_userId` (`userId`) USING BTREE,
    KEY `idx_spaceName` (`spaceName`) USING BTREE,
    KEY `idx_spaceLevel` (`spaceLevel`) USING BTREE,
    KEY `idx_spaceType` (`spaceType`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间';

SET FOREIGN_KEY_CHECKS = 1;
