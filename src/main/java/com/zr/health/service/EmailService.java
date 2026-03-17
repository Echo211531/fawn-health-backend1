package com.zr.health.service;

/**
 * 邮件服务接口
 * 提供发送各类邮件的功能，包括验证码、通知等
 */
public interface EmailService {
    /**
     * 发送邮箱验证码
     *
     * @param email     接收验证码的邮箱地址
     * @param emailCode 要发送的验证码内容
     * @throws IllegalArgumentException 如果邮箱格式不合法
     * @throws RuntimeException         如果发送过程中出现异常
     */
    void sendVerificationCode(String email, String emailCode);
}