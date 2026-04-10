/*
 Navicat Premium Dump SQL

 Source Server         : localhost_80
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : localhost:3306
 Source Schema         : picture

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 03/09/2025 18:21:16
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for picture
-- ----------------------------
DROP TABLE IF EXISTS `picture`;
CREATE TABLE `picture`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片 url',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片名称',
  `introduction` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简介',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类',
  `tags` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '标签（JSON 数组）',
  `picSize` bigint NULL DEFAULT NULL COMMENT '图片体积',
  `picWidth` int NULL DEFAULT NULL COMMENT '图片宽度',
  `picHeight` int NULL DEFAULT NULL COMMENT '图片高度',
  `picScale` double NULL DEFAULT NULL COMMENT '图片宽高比例',
  `picFormat` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片格式',
  `userId` bigint NOT NULL COMMENT '创建用户 id',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `reviewStatus` int NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
  `reviewMessage` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核信息',
  `reviewerId` bigint NULL DEFAULT NULL COMMENT '审核人 ID',
  `reviewTime` datetime NULL DEFAULT NULL COMMENT '审核时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_introduction`(`introduction` ASC) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_tags`(`tags` ASC) USING BTREE,
  INDEX `idx_userId`(`userId` ASC) USING BTREE,
  INDEX `idx_reviewStatus`(`reviewStatus` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1959997758270148610 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '图片' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of picture
-- ----------------------------
INSERT INTO `picture` VALUES (1957001938583285761, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com//public/1945506895712337922/2025-08-17_fjn0erbx84o0s32o.jpg', '20230927_12134727.png', '派大星健身励志', '海报', '[\"派大星\",\"健身\"]', 86977, 1125, 633, 1.78, 'JPEG', 1945506895712337922, '2025-08-17 16:49:56', '2025-08-17 08:50:28', '2025-08-25 23:12:48', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:48');
INSERT INTO `picture` VALUES (1957002213339557889, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com//public/1945506895712337922/2025-08-17_9hy2lot2ptzwylvl.jpg', '20230926_17132265.png', '蜡笔小新抱着小白。', '表情包', '[\"蜡笔小新\",\"小白\"]', 7636, 1080, 1080, 1, 'heif', 1945506895712337922, '2025-08-17 16:51:01', '2025-08-17 08:51:43', '2025-08-17 19:59:00', 1, 0, NULL, NULL, NULL);
INSERT INTO `picture` VALUES (1957005731194527745, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com//public/1945506895712337922/2025-08-17_btkfmgqh0pmj83n0.jpg', '20231006_00211546.png', '派大星生气了！', '表情包', '[\"派大星\",\"搞笑\"]', 32671, 716, 716, 1, 'JPEG', 1945506895712337922, '2025-08-17 17:05:00', '2025-08-17 09:05:45', '2025-08-25 23:20:18', 0, 2, '管理员操作拒绝', 1945506895712337922, '2025-08-25 15:20:18');
INSERT INTO `picture` VALUES (1957364004686929921, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-18_vwtt8axbz3oncps7.png', '蜡笔小新开车2025-08-18 170920', '土豪小新', '表情包', '[\"蜡笔小新\",\"搞笑\",\"热门\"]', 145577, 434, 432, 1, 'PNG', 1945506895712337922, '2025-08-18 16:48:39', '2025-08-18 09:13:10', '2025-08-25 23:12:47', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:48');
INSERT INTO `picture` VALUES (1958059981592981505, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_xa1gh46jx3rfx2ko.png', 'Drake', 'Drake!', '表情包', '[\"高清\",\"Drake\",\"Lamar\"]', 677455, 972, 965, 1.01, 'PNG', 1945506895712337922, '2025-08-20 14:54:13', '2025-08-20 07:39:42', '2025-08-25 23:12:47', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:47');
INSERT INTO `picture` VALUES (1958072257687433218, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_6pphmsqihnkehet5.png', 'Kanye', 'KanyeWest!', '海报', '[\"艺术\",\"生活\",\"Kanye West\"]', 845840, 1074, 827, 1.3, 'PNG', 1945506895712337922, '2025-08-20 15:43:00', '2025-08-20 07:43:27', '2025-08-25 23:12:46', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:47');
INSERT INTO `picture` VALUES (1958072475594108929, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_qvlp8mp0193i84nl.png', 'Eddie Peng', '作者本人', '素材', '[\"高清\",\"彭于晏\",\"作者本人\"]', 960835, 982, 984, 1, 'PNG', 1945506895712337922, '2025-08-20 15:43:52', '2025-08-20 07:44:27', '2025-08-25 23:12:46', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:47');
INSERT INTO `picture` VALUES (1958072713037852673, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_kma6h3s69olyt19u.png', 'Kanyee', '坎爷韦斯特特特，你怎么回事呢呢呢？', '表情包', '[\"Kanyee\",\"搞笑\",\"咖啡豆\"]', 540314, 799, 881, 0.91, 'PNG', 1945506895712337922, '2025-08-20 15:44:48', '2025-08-20 07:46:03', '2025-08-25 23:12:46', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:46');
INSERT INTO `picture` VALUES (1958073144535265282, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_9yznsikp0rehaghw.png', '网站icon', '网站icon', '素材', '[\"蜡笔小新\"]', 223408, 697, 698, 1, 'PNG', 1945506895712337922, '2025-08-20 15:46:31', '2025-08-20 07:46:50', '2025-08-25 23:12:45', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:45');
INSERT INTO `picture` VALUES (1958073258188320769, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_s4tiq7dl3pr7ncar.png', '悍匪派大星', '悍匪派大星', '表情包', '[\"派大星\",\"搞笑\"]', 320265, 980, 980, 1, 'PNG', 1945506895712337922, '2025-08-20 15:46:58', '2025-08-20 07:47:11', '2025-08-25 23:12:44', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:45');
INSERT INTO `picture` VALUES (1958073369786167298, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_rglvn509cmgufjij.png', '蜡笔小新', '嘘嘘~', '表情包', '[\"蜡笔小新\",\"搞笑\"]', 836097, 932, 931, 1, 'PNG', 1945506895712337922, '2025-08-20 15:47:25', '2025-08-20 07:47:50', '2025-08-25 23:12:44', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:44');
INSERT INTO `picture` VALUES (1958119276321828865, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1945506895712337922/2025-08-20_5n81dr3237n2m7ic.png', '鳄鱼山先生', '鳄鱼山先生。', '表情包', '[\"蜡笔小新\",\"鳄鱼山先生\"]', 369645, 720, 720, 1, 'PNG', 1945506895712337922, '2025-08-20 18:49:50', '2025-08-20 10:50:13', '2025-08-25 23:12:34', 0, 1, '管理员操作通过', 1945506895712337922, '2025-08-25 15:12:35');
INSERT INTO `picture` VALUES (1959997758270148609, 'https://xcw-1373545852.cos.ap-guangzhou.myqcloud.com/public/1949873190859730945/2025-08-25_vleuolz266s4ewbs.png', '蜡笔小新壁纸', '111', '素材', '[\"蜡笔小新\",\"壁纸\"]', 647103, 1112, 620, 1.79, 'PNG', 1949873190859730945, '2025-08-25 23:14:15', '2025-08-25 15:14:38', '2025-08-25 23:14:58', 0, 2, '管理员操作拒绝', 1945506895712337922, '2025-08-25 15:14:58');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `userAccount` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '账号',
  `userPassword` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `userName` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `userAvatar` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `userProfile` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户简介',
  `userRole` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin',
  `editTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '编辑时间',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_userAccount`(`userAccount` ASC) USING BTREE,
  INDEX `idx_userName`(`userName` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1949873190859730946 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1945506895712337922, 'eddie', 'a6d8ccf352674a2a2d255f649d8c58d1', 'Eddie', 'https://assets.leetcode.cn/aliyun-lc-upload/users/vigorous-jennings4mh/avatar_1703062532.png', '我是云图库项目的管理员。', 'admin', '2025-07-16 23:32:44', '2025-07-16 23:32:44', '2025-08-03 22:14:51', 0);
INSERT INTO `user` VALUES (1949873190859730945, 'jackson', 'a6d8ccf352674a2a2d255f649d8c58d1', 'JackSon', 'https://img.alicdn.com/sns_logo/i2/4093866517/O1CN01MI2Ti71y0qAyfmDIj_!!4093866517-0-userheaderimgshow.jpg', '为者常成，行者常至！', 'user', '2025-07-29 00:42:50', '2025-07-29 00:42:50', '2025-08-17 18:08:24', 0);

SET FOREIGN_KEY_CHECKS = 1;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId bigint null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);


