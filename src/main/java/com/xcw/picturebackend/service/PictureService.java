package com.xcw.picturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcw.picturebackend.model.dto.picture.PictureUploadRequest;
import com.xcw.picturebackend.model.entity.Picture;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 20339
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-08-10 13:27:02
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param multipartFile 上传的文件
     * @param pictureUploadRequest 上传请求参数
     * @param loginUser 当前登录用户
     * @return 图片信息
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

}
