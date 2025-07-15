package com.xcw.picturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.service.UserService;
import com.xcw.picturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 20339
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-07-15 19:11:18
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




