package com.xcw.picturebackend.service;

import com.xcw.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 20339
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-07-15 19:11:18
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);
}
