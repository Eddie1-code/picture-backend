package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.picture.PictureQueryRequest;
import com.xcw.picturebackend.model.dto.picture.PictureReviewRequest;
import com.xcw.picturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.xcw.picturebackend.model.dto.picture.PictureUploadRequest;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

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
     *  校验图片信息
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
     * @param picture  图片审核请求
     * @param loginUser 已登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量上传请求
     * @param loginUser  已登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

}

