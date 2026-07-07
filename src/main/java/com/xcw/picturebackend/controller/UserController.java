package com.xcw.picturebackend.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcw.picturebackend.annotation.AuthCheck;
import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.DeleteRequest;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.config.CosClientConfig;
import com.xcw.picturebackend.constant.UserConstant;
import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.exception.ThrowUtils;
import com.xcw.picturebackend.manager.CosManager;
import com.xcw.picturebackend.manager.RateLimitManager;
import com.xcw.picturebackend.model.dto.user.*;

import com.xcw.picturebackend.model.entity.User;
import com.xcw.picturebackend.model.vo.LoginUserVO;
import com.xcw.picturebackend.model.vo.UserVO;
import com.xcw.picturebackend.manager.auth.StpKit;
import com.xcw.picturebackend.service.UserService;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private com.xcw.picturebackend.manager.social.UserOnlineManager userOnlineManager;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RateLimitManager rateLimitManager;

    @Resource
    private com.xcw.picturebackend.security.RequestIpResolver requestIpResolver;

    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public BaseResponse<Map<String, String>> getCaptcha() {
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(120, 40, 4, 4);
        String code = captcha.getCode();
        String captchaId = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set("captcha:" + captchaId, code, 120, TimeUnit.SECONDS);
        Map<String, String> result = new HashMap<>();
        result.put("captchaId", captchaId);
        result.put("captchaImage", captcha.getImageBase64Data());
        return ResultUtils.success(result);
    }

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录接口
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        // 把当前 token 放在响应头，前端可按标签页存储到 sessionStorage
        response.setHeader(StpKit.SPACE.getTokenName(), StpKit.SPACE.getTokenValue());
        // 同时返回标准化的 token 名和值，前端可动态读取并回传
        response.setHeader("x-sa-token-name", StpKit.SPACE.getTokenName());
        response.setHeader("x-sa-token-value", StpKit.SPACE.getTokenValue());

        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 在线心跳：客户端建议 60s 一次，后端将 TTL 保持在 120s
     */
    @PostMapping("/heartbeat")
    public BaseResponse<Boolean> heartbeat(HttpServletRequest request) {
        try {
            User loginUser = userService.getLoginUser(request);
            userOnlineManager.heartbeat(loginUser.getId());
            return ResultUtils.success(true);
        } catch (Exception e) {
            return ResultUtils.success(false);
        }
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 仅管理员可以添加用户
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        //默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户添加失败");
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User existing = userService.getById(userUpdateRequest.getId());
        ThrowUtils.throwIf(existing == null, ErrorCode.NOT_FOUND_ERROR);
        User user = new User();
        user.setId(userUpdateRequest.getId());
        user.setUserName(StrUtil.isNotBlank(userUpdateRequest.getUserName())
                ? userUpdateRequest.getUserName() : existing.getUserName());
        user.setUserAvatar(StrUtil.isNotBlank(userUpdateRequest.getUserAvatar())
                ? userUpdateRequest.getUserAvatar() : existing.getUserAvatar());
        user.setUserProfile(userUpdateRequest.getUserProfile() != null
                ? userUpdateRequest.getUserProfile() : existing.getUserProfile());
        user.setUserRole(StrUtil.isNotBlank(userUpdateRequest.getUserRole())
                ? userUpdateRequest.getUserRole() : existing.getUserRole());
        if (StrUtil.isNotBlank(userUpdateRequest.getUserPassword())) {
            ThrowUtils.throwIf(userUpdateRequest.getUserPassword().length() < 8,
                    ErrorCode.PARAMS_ERROR, "密码长度不能小于 8");
            user.setUserPassword(userService.getEncryptPassword(userUpdateRequest.getUserPassword()));
        } else {
            user.setUserPassword(existing.getUserPassword());
        }
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 兑换会员
     */
    @PostMapping("/exchange/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipExchangeRequest vipExchangeRequest,
                                             HttpServletRequest httpServletRequest) {
        ThrowUtils.throwIf(vipExchangeRequest == null, ErrorCode.PARAMS_ERROR);
        String vipCode = vipExchangeRequest.getVipCode();
        User loginUser = userService.getLoginUser(httpServletRequest);

        // 调用 service 层的方法进行会员兑换
        boolean result = userService.exchangeVip(loginUser, vipCode);
        return ResultUtils.success(result);
    }

    /**
     * 查询当前登录用户的隐私设置（6 个布尔位）。
     */
    @GetMapping("/privacy/my")
    public BaseResponse<UserVO> getMyPrivacy(HttpServletRequest request) {
        return ResultUtils.success(userService.getMyPrivacy(request));
    }

    /**
     * 更新当前登录用户的隐私设置：字段为 null 表示保持不变。
     */
    @PostMapping("/privacy/update")
    public BaseResponse<UserVO> updateMyPrivacy(@RequestBody UserPrivacyUpdateRequest req,
                                                HttpServletRequest request) {
        return ResultUtils.success(userService.updateMyPrivacy(req, request));
    }

    /**
     * 用户中心更新用户信息
     */
    @PostMapping("/update/my")
    public BaseResponse<UserVO> updateMyProfile(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        userUpdateRequest.setId(loginUser.getId());
        // 不允许用户自己修改userRole
        userUpdateRequest.setUserRole(loginUser.getUserRole());
        UserVO updatedUser = userService.updateMyProfile(userUpdateRequest, request);
        return ResultUtils.success(updatedUser);
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile multipartFile, HttpServletRequest request) {
        // 检查用户是否登录
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        // 检查文件是否为空
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 生成唯一文件名
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = StrUtil.blankToDefault(FileUtil.getSuffix(originalFilename), "").toLowerCase(Locale.ROOT);
        final List<String> allowSuffixList = Arrays.asList("jpg", "jpeg", "png", "webp");
        ThrowUtils.throwIf(!allowSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "仅支持 JPG / PNG / WebP");
        // 统一 jpeg -> jpg，避免同类型出现两种后缀
        if ("jpeg".equals(suffix)) {
            suffix = "jpg";
        }
        String filename = UUID.randomUUID().toString() + "." + suffix;

        // 构建文件路径
        String filepath = String.format("avatar/%s/%s", loginUser.getId(), filename);
        File file = null;

        try {
            // 上传文件
            file = File.createTempFile("avatar_", "." + suffix);
            multipartFile.transferTo(file);
            /*
             * 与图库上传（PictureUploadTemplate）一致：走 COS 数据万象 putPictureObject，
             * 会生成 WebP 压缩对象；返回地址应为该处理结果（与图库 picUrl 一致）。
             * 此前仅用普通 putObject 存 jpg/png 时，在部分 CDN/桶配置下无法展示，而直接上传 webp 却正常。
             */
            PutObjectResult putObjectResult = null;
            try {
                putObjectResult = cosManager.putPictureObject(filepath, file);
            } catch (Exception ex) {
                log.warn("头像走万象处理失败，回退直传: {}", ex.getMessage());
            }
            if (putObjectResult != null && putObjectResult.getCiUploadResult() != null) {
                ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
                if (processResults != null && CollUtil.isNotEmpty(processResults.getObjectList())) {
                    CIObject compressed = processResults.getObjectList().get(0);
                    String key = compressed.getKey();
                    if (StrUtil.isNotBlank(key)) {
                        String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
                        String fullUrl = StrUtil.addSuffixIfNot(cosClientConfig.getCdnDomain(), "/") + normalizedKey;
                        return ResultUtils.success(fullUrl);
                    }
                }
                // 万象未返回处理列表时，原图已在 putPictureObject 中上传至 filepath（与 PictureUploadTemplate 回退一致）
                String fullUrl = StrUtil.addSuffixIfNot(cosClientConfig.getCdnDomain(), "/") + filepath;
                return ResultUtils.success(fullUrl);
            }
            // 万象完全未生效时回退直传
            String contentType = resolveAvatarContentType(suffix, multipartFile.getContentType());
            cosManager.putObject(filepath, file, contentType);
            String fullUrl = StrUtil.addSuffixIfNot(cosClientConfig.getCdnDomain(), "/") + filepath;
            return ResultUtils.success(fullUrl);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                file.delete();
            }
        }
    }

    // ========== 邮箱绑定 ==========

    /**
     * 发送邮箱验证码
     */
    @PostMapping("/email/code")
    public BaseResponse<Boolean> sendEmailCode(@RequestBody UserUpdateRequest req, HttpServletRequest request) {
        ThrowUtils.throwIf(req == null || StrUtil.isBlank(req.getEmail()), ErrorCode.PARAMS_ERROR, "邮箱不能为空");
        User loginUser = userService.getLoginUser(request);
        userService.sendEmailCode(loginUser.getId(), req.getEmail());
        return ResultUtils.success(true);
    }

    /**
     * 绑定邮箱
     */
    @PostMapping("/email/bind")
    public BaseResponse<Boolean> bindEmail(@RequestBody UserUpdateRequest req, HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.bindEmail(loginUser.getId(), req.getEmail(), req.getEmailCode());
        return ResultUtils.success(true);
    }

    // ========== 忘记密码 ==========

    /**
     * 忘记密码 - 请求发送重置邮件
     */
    @PostMapping("/password/reset-request")
    public BaseResponse<Boolean> requestPasswordReset(@RequestBody PasswordResetRequest req,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        // IP 限流：单 IP 每小时 10 次
        String clientIp = requestIpResolver.resolveClientIp(request);
        RateLimitManager.RateLimitDecision ipCheck =
                rateLimitManager.checkRouteUserIpRateLimit("pwd_reset_req", null, clientIp, 10, 3600, false);
        if (!ipCheck.isAllowed()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "操作过于频繁，请稍后再试");
        }
        userService.sendPasswordResetEmail(req.getUserAccount(), req.getCaptchaId(), req.getCaptchaCode());
        // 不管账号是否存在，统一返回成功（防账号枚举）
        return ResultUtils.success(true);
    }

    /**
     * 忘记密码 - 执行重置
     */
    @PostMapping("/password/reset")
    public BaseResponse<Boolean> executePasswordReset(@RequestBody PasswordResetExecuteRequest req,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        // IP 限流：单 IP 每分钟 5 次（防 token 暴力尝试）
        String clientIp = requestIpResolver.resolveClientIp(request);
        RateLimitManager.RateLimitDecision ipCheck =
                rateLimitManager.checkRouteUserIpRateLimit("pwd_reset_exec", null, clientIp, 5, 60, false);
        if (!ipCheck.isAllowed()) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "操作过于频繁，请稍后再试");
        }
        userService.resetPassword(req.getToken(), req.getNewPassword(), req.getCheckPassword());
        return ResultUtils.success(true);
    }

    // ========== 修改密码 ==========

    /**
     * 已登录用户修改密码
     */
    @PostMapping("/password/change")
    public BaseResponse<Boolean> changePassword(@RequestBody ChangePasswordRequest req, HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.changePassword(loginUser.getId(), req.getOldPassword(), req.getNewPassword(), req.getCheckPassword());
        return ResultUtils.success(true);
    }

    /**
     * 解析头像上传的 Content-Type：优先使用浏览器提供的 image/*；对空值、application/*、错误的 image/jpg 按扩展名兜底。
     */
    private static String resolveAvatarContentType(String fileSuffix, String multipartContentType) {
        String bySuffix = mapFileSuffixToImageMime(fileSuffix);
        if (StrUtil.isBlank(multipartContentType)) {
            return bySuffix;
        }
        String ct = multipartContentType.toLowerCase(Locale.ROOT).trim();
        if ("image/jpg".equals(ct)) {
            return "image/jpeg";
        }
        if (ct.startsWith("image/")) {
            return ct;
        }
        return bySuffix;
    }

    private static String mapFileSuffixToImageMime(String suffix) {
        if (StrUtil.isBlank(suffix)) {
            return "application/octet-stream";
        }
        switch (suffix.toLowerCase(Locale.ROOT)) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

}
