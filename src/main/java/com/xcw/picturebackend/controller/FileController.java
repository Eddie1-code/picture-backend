package com.xcw.picturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.xcw.picturebackend.annotation.AuthCheck;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.constant.UserConstant;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.manager.CosManager;
import com.xcw.picturebackend.manager.upload.FilePictureUpload;
import com.xcw.picturebackend.model.dto.file.UploadPictureResult;
import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UserService userService;

    /**
     * 测试文件上传
     *
     * @param multipartFile 文件
     * @return 文件路径
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 帖子配图上传
     * <p>
     * - 登录即可使用，路径前缀按 userId 区分
     * - 走 PictureUploadTemplate 的压缩 + 缩略图流水线；url 落 COS CDN，不写 picture 表
     * - 返回 Map 便于前端直接拿 url / thumbnailUrl / 尺寸信息
     */
    @PostMapping("/post/image")
    public BaseResponse<Map<String, Object>> uploadPostImage(
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        String prefix = String.format("post/%s", loginUser.getId());
        UploadPictureResult result = filePictureUpload.uploadPicture(multipartFile, prefix);
        Map<String, Object> body = new HashMap<>();
        body.put("url", result.getUrl());
        body.put("thumbnailUrl", result.getThumbnailUrl());
        body.put("picWidth", result.getPicWidth());
        body.put("picHeight", result.getPicHeight());
        body.put("picSize", result.getPicSize());
        body.put("picFormat", result.getPicFormat());
        return ResultUtils.success(body);
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

}
