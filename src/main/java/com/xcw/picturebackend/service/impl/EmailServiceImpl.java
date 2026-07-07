package com.xcw.picturebackend.service.impl;

import com.xcw.picturebackend.exception.BusinessException;
import com.xcw.picturebackend.exception.ErrorCode;
import com.xcw.picturebackend.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Value("${spring.mail.properties.from-name:栖图}")
    private String fromName;

    @Override
    @Async
    public void sendVerificationCode(String to, String code) {
        String subject = "【栖图】邮箱验证码";
        String content = buildHtml("邮箱验证",
                "<p>您的验证码是：</p>" +
                "<h2 style=\"color:#1677ff;letter-spacing:4px;\">" + code + "</h2>" +
                "<p>验证码 5 分钟内有效，请勿转发给他人。</p>" +
                "<p>如非本人操作，请忽略此邮件。</p>");
        send(to, subject, content);
    }

    @Override
    @Async
    public void sendPasswordResetLink(String to, String resetUrl) {
        String subject = "【栖图】密码重置";
        String content = buildHtml("密码重置",
                "<p>您正在为栖图账号重置密码，请点击下方按钮：</p>" +
                "<p style=\"margin:24px 0;\">" +
                "<a href=\"" + resetUrl + "\" " +
                "style=\"background:#1677ff;color:#fff;padding:10px 24px;border-radius:6px;text-decoration:none;\">" +
                "重置密码</a></p>" +
                "<p>或复制链接到浏览器打开：<br>" + resetUrl + "</p>" +
                "<p>此链接 15 分钟内有效，仅能使用一次。</p>" +
                "<p>如非本人操作，请忽略此邮件。</p>");
        send(to, subject, content);
    }

    @Override
    @Async
    public void sendPasswordResetNotice(String to) {
        String subject = "【栖图】密码已重置";
        String content = buildHtml("安全通知",
                "<p>您的栖图账号密码刚刚被重置。</p>" +
                "<p>如非本人操作，请立即登录并修改密码，或联系管理员。</p>");
        send(to, subject, content);
    }

    private void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "邮件发送失败，请稍后重试");
        }
    }

    private String buildHtml(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head>" +
                "<body style=\"font-family:Arial,sans-serif;padding:24px;\">" +
                "<h2>" + title + "</h2>" +
                body +
                "<hr style=\"margin-top:32px;border:none;border-top:1px solid #eee;\">" +
                "<p style=\"color:#999;font-size:12px;\">此邮件由栖图系统自动发送，请勿回复。</p>" +
                "</body></html>";
    }
}
