package com.xcw.picturebackend.service;

import com.xcw.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

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
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HttpServletRequest 对象
     * @return 登录成功的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏后的登录用户的视图对象
     *
     * @param user 用户实体
     * @return 登录用户视图对象
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return  已登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request HttpServletRequest 对象
     * @return 是否注销成功
     */
    boolean userLogout(HttpServletRequest request);

}
