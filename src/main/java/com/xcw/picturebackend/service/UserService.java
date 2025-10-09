package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xcw.picturebackend.model.dto.user.UserQueryRequest;
import com.xcw.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.vo.LoginUserVO;
import com.xcw.picturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户
     * @return 用户实体
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户列表
     *
     * @param userList 用户实体
     * @return 脱敏后的用户视图对象
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件的 QueryWrapper
     *
     * @param userQueryRequest 查询请求对象
     * @return QueryWrapper<User> 查询条件包装器
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

//    /**
//     * 用户中心更新用户信息
//     *
//     * @param userUpdateRequest 用户更新请求
//     * @return  更新后的用户
//     */
//    public UserVO updateMyProfile(UserUpdateRequest userUpdateRequest, HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user 用户
     * @return 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 用户兑换会员
     * @param user 当前用户
     * @param vipCode 会员兑换码
     * @return 是否兑换成功
     */
    boolean exchangeVip(User user, String vipCode);
}
