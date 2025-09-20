package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xcw.picturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xcw.picturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xcw.picturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author EddieXu
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-09-19 18:09:53
*/
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 创建空间成员
     * @param spaceUserAddRequest 创建空间成员请求
     * @return 空间成员id
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);


    /**
     * 校验空间成员 spaceUser是否合法
     *
     * @param spaceUser 空间成员
     * @param add   是否为添加操作
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 获取空间成员信息(脱敏处理)
     *
     * @param spaceUser   空间成员实体
     * @param request 请求
     * @return 脱敏后的空间成员信息
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);


    /**
     * 获取空间成员分页信息
     *
     * @param spaceUserList 空间成员列表
     * @return 分页后的空间成员信息列表
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 获取空间成员查询条件
     *
     * @param spaceUserQueryRequest 查询请求参数
     * @return QueryWrapper<space> 查询条件包装器
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
