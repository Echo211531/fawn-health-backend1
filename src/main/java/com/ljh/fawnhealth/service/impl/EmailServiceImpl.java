package com.ljh.fawnhealth.service.impl;

import com.ljh.fawnhealth.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送邮箱验证码
     * @param email 接收邮箱
     * @param emailCode 验证码
     */
    @Override
    public void sendVerificationCode(String email, String emailCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("小鹿健康-邮箱验证");
        message.setText(
                "尊敬的用户您好：\n\n" +
                        "您正在进行邮箱验证，本次验证码为：" + emailCode + "，请在5分钟内使用。\n\n" +
                        "如非本人操作，请忽略此邮件。\n\n" +
                        "--小鹿健康平台"
        );
        mailSender.send(message);
    }
}




