package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.picture.*;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 20339
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-08-10 13:27:02
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource          文件输入源，可以是 MultipartFile 或 String（URL）
     * @param pictureUploadRequest 上传请求参数
     * @param loginUser            当前登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片查询条件
     *
     * @param pictureQueryRequest 查询请求参数
     * @return QueryWrapper<Picture> 查询条件包装器
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片信息(脱敏处理)
     *
     * @param picture 图片实体
     * @param request 请求
     * @return 脱敏后的图片信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);


    /**
     * 获取图片分页信息
     *
     * @param picturePage 分页对象
     * @param request     请求
     * @return 分页后的图片信息列表
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 校验图片信息
     *
     * @param picture 图片实体
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest 图片审核请求
     * @param loginUser            已登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充图片审核相关参数
     *
     * @param picture   图片审核请求
     * @param loginUser 已登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param loginUser                   已登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 分页获取图片列表（封装类,有缓存）
     *
     * @param pictureQueryRequest 图片查询请求
     * @param request             请求
     * @return 图片分页列表
     */
    Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 删除图片文件（包括数据库记录和存储桶中的文件）
     *
     * @param oldPicture 需要删除的图片实体
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 删除图片（包括数据库记录和存储桶中的文件）
     *
     * @param pictureId 图片id
     * @param loginUser 已登录用户
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 编辑图片信息
     *
     * @param pictureEditRequest 图片编辑请求
     * @param loginUser          已登录用户
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);


    /**
     * 检查空间图片权限
     *
     * @param loginUser 已登录用户
     * @param picture   图片实体
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 通过颜色搜索图片
     *
     * @param spaceId   空间id
     * @param picColor  颜色值
     * @param loginUser 已登录用户
     * @return 图片列表
     */
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量编辑图片信息
     *
     * @param pictureEditByBatchRequest 批量编辑请求
     * @param loginUser                 已登录用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);
}

