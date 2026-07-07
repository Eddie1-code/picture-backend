package com.xcw.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 已登录用户修改密码
 */
@Data
public class ChangePasswordRequest implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

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
