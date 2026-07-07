package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 - 请求发送重置邮件
 */
@Data
public class PasswordResetRequest implements Serializable {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 图形验证码ID
     */
    private String captchaId;

    /**
     * 图形验证码
     */
    private String captchaCode;

    private static final long serialVersionUID = 1L;
}
