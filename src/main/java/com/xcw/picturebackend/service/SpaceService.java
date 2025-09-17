package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.model.dto.space.SpaceAddRequest;
import com.xcw.picturebackend.model.dto.space.SpaceQueryRequest;
import com.xcw.picturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author EddieXu
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-09-10 19:32:56
 */
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     * @param spaceAddRequest 创建空间请求
     * @param loginUser  已登录用户
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);


    /**
     * 校验space是否合法
     *
     * @param space 空间
     * @param add   是否为添加操作
     */
    void validSpace(Space space, boolean add);

    /**
     * 获取空间信息(脱敏处理)
     *
     * @param space   空间实体
     * @param request 请求
     * @return 脱敏后的空间信息
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 获取空间分页信息
     *
     * @param spacePage 分页对象
     * @param request   请求
     * @return 分页后的空间信息列表
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取空间查询条件
     *
     * @param spaceQueryRequest 查询请求参数
     * @return QueryWrapper<space> 查询条件包装器
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 根据空间级别，自动填充限额
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 检查用户对空间的访问权限
     *
     * @param loginUser 已登录用户
     * @param space     空间
     */
    void checkSpaceAuth(User loginUser, Space space);
}
