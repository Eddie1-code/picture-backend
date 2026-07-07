package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 忘记密码 - 执行重置
 */
@Data
public class PasswordResetExecuteRequest implements Serializable {

    /**
     * 重置令牌
     */
    private String token;

    /**
     * 新密码
     */
    private String newPassword;

    /**
     * 确认新密码
     */
    private String checkPassword;

    private static final long serialVersionUID = 1L;
}
