package com.xcw.picturebackend.utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码迁移工具：生成 BCrypt 哈希值用于手动更新数据库。
 * 用法：在 IDEA 中右键 Run，或 mvn exec:java
 */
public class BcryptHashGenerator {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("用法: java BcryptHashGenerator <你的密码>");
            System.out.println("示例: java BcryptHashGenerator mypassword123");
            System.exit(1);
        }
        String password = args[0];
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        System.out.println("BCrypt 哈希值:");
        System.out.println(hash);
        System.out.println();
        System.out.println("SQL 语句:");
        System.out.println("UPDATE user SET user_password = '" + hash + "' WHERE user_account = '你的账号';");
    }
}
