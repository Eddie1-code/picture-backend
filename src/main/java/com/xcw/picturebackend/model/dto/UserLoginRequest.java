package com.xcw.picturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 * 用于接收用户登录时的账号和密码信息
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 4861346868105052360L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
