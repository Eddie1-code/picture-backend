package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    /**
     * 序列化ID
     */
    private static final long serialVersionUID = 5252064330873604535L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 用户昵称（必填）
     */
    private String userName;

    /**
     * 个人简介（可选）
     */
    private String userProfile;

    /**
     * 头像 URL（可选；注册阶段可空，登录后在个人中心上传）
     */
    private String userAvatar;

    /**
     * 验证码ID（对应Redis key）
     */
    private String captchaId;

    /**
     * 用户输入的验证码
     */
    private String captchaCode;

    /**
     * 邮箱（可选绑定）
     */
    private String email;

    /**
     * 邮箱验证码
     */
    private String emailCode;
}
