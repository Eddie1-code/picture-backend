package com.xcw.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.constant.UserConstant;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.model.dto.user.UserQueryRequest;
import com.xcw.picturebackend.model.dto.user.UserUpdateRequest;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.enums.UserRoleEnum;
import com.xcw.picturebackend.model.vo.LoginUserVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.service.UserService;
import com.xcw.picturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 20339
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-07-15 19:11:18
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }

        //传统写法：
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度小于4");
        }
        //改进写法：
        // ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度过短");


        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度小于8");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }

        //2.检查用户账号是否和数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }

        //3.密码一定要加盐加密
        String encryptedPassword = getEncryptPassword(userPassword);

        //4.将用户信息存入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptedPassword);
        user.setUserName("无名"); //默认用户名
        user.setUserRole(UserRoleEnum.USER.getValue()); //默认用户角色
        boolean saveResult = this.save(user);
        /**
         * if(!saveResult) {
         *      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户注册失败");
         * }
         *
         */
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "用户注册失败");

        return user.getId(); //返回新用户的ID
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HttpServletRequest 对象
     * @return 登录成功的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        //改进写法：
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号错误");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码错误");

        //2.对用户传递的密码进行加密
        String encryptedPassword = getEncryptPassword(userPassword);

        //3.查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount)
                .eq("userPassword", encryptedPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            //用户不存在，抛异常
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }

        //4.保存用户的登录状态到Session中
//        request.getSession().setAttribute("user_login_state", user);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user); //返回登录用户信息对象
    }


    /**
     * 加密用户密码
     *
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        //加盐，混淆密码
        final String SALT = "xcw";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取脱敏后的登录用户的视图对象
     *
     * @param user 用户实体
     * @return 登录用户视图对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null; //如果用户不存在，返回null
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        //使用BeanUtil.copyProperties进行属性拷贝
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 已登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //先判断是否登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        ThrowUtils.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

        //从数据库中查询（追求性能的话，直接返回上述结果）
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            log.info("getLoginUser failed, userId cannot match user");
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或已注销");
        }
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request HttpServletRequest 对象
     * @return 是否注销成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //先判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            log.info("user logout failed, user not logged in");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录或已注销");
        }
        //如果登录了，清除Session中的用户信息（移除登录状态）
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true; //返回注销成功
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户
     * @return 脱敏后的用户视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获得脱敏后的用户列表
     *
     * @param userList 用户实体
     * @return 脱敏后的用户视图对象列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
//        int current = userQueryRequest.getCurrent();
//        int pageSize = userQueryRequest.getPageSize();
//        String sortField = userQueryRequest.getSortField();
//        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //添加查询条件
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        return queryWrapper;
    }

//    /**
//     * 用户个人中心修改用户个人信息
//     *
//     * @param userUpdateRequest 用户更新请求对象
//     * @param request           HttpServletRequest 对象
//     * @return 是否更新成功
//     */
//    @Override
//    public UserVO updateMyProfile(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
//        if (userUpdateRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "更新请求不能为空");
//        }
//        //1.获取当前登录用户
//        User currentUser = this.getLoginUser(request);
//        if (currentUser == null) {
//            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录或已注销");
//        }
//        //2.校验更新请求参数
//        Long id = userUpdateRequest.getId();
//        if (id == null || !id.equals(currentUser.getId())) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不匹配");
//        }
//        String userName = userUpdateRequest.getUserName();
//        String userPassword = userUpdateRequest.getUserPassword();
//        String userAvatar = userUpdateRequest.getUserAvatar();
//        String userProfile = userUpdateRequest.getUserProfile();
//
//        //3.更新用户信息
//        User userToUpdate = new User();
//        userToUpdate.setId(id);
//        userToUpdate.setUserName(userName);
//        if (StrUtil.isNotBlank(userPassword)) {
//            //如果用户密码不为空，则加密后更新
//            userToUpdate.setUserPassword(getEncryptPassword(userPassword));
//        }
//        userToUpdate.setUserAvatar(userAvatar);
//        userToUpdate.setUserProfile(userProfile);
//        boolean updateResult = this.updateById(userToUpdate);
//        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "用户信息更新失败");
//        //4.更新成功后，返回更新后的用户信息
//        User updatedUser = this.getById(id);
//        if (updatedUser == null) {
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新后的用户信息不存在");
//        }
//        //返回脱敏后的用户信息
//        return this.getUserVO(updatedUser);
//    }


}




