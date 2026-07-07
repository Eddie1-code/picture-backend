package com.xcw.picturebackend.service;

/**
 * 邮件发送服务
 */
public interface EmailService {

    /**
     * 发送邮箱验证码
     * @param to 收件人邮箱
     * @param code 6 位数字验证码
     */
    void sendVerificationCode(String to, String code);

    /**
     * 发送密码重置链接
     * @param to 收件人邮箱
     * @param resetUrl 含 token 的重置链接
     */
    void sendPasswordResetLink(String to, String resetUrl);

    /**
     * 发送密码已重置通知
     * @param to 收件人邮箱
     */
    void sendPasswordResetNotice(String to);
}
