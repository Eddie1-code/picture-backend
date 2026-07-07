-- 用户表添加邮箱字段（可空，用于忘记密码）
ALTER TABLE `user`
    ADD COLUMN `email` varchar(256) DEFAULT NULL COMMENT '邮箱' AFTER `userAccount`;
