package com.xcw.picturebackend.controller;

import com.xcw.picturebackend.common.BaseResponse;
import com.xcw.picturebackend.common.ResultUtils;
import com.xcw.picturebackend.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/")
public class MainController {

    @Resource
    private EmailService emailService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

    /**
     * 测试邮件发送（仅用于验证邮件配置是否生效）
     */
    @PostMapping("/health/email")
    public BaseResponse<String> testEmail(@RequestParam String to) {
        emailService.sendVerificationCode(to, "123456");
        return ResultUtils.success("已发送测试邮件到 " + to);
    }
}
