package com.ljh.fawnhealth.service.impl;

import com.ljh.fawnhealth.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

/**
 * 邮件服务实现类
 * 提供发送各类邮件的功能，包括验证码、通知等
 */
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送邮箱验证码（HTML格式）
     *
     * @param email     接收邮箱地址
     * @param emailCode 验证码内容
     * @throws RuntimeException 如果邮件发送失败
     */
    @Override
    public void sendVerificationCode(String email, String emailCode) {
        try {
            // 创建MIME消息（Jakarta EE版本）
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("小鹿健康 - 邮箱验证");

            Context context = new Context();
            context.setVariable("emailCode", emailCode);
            context.setVariable("appName", "小鹿健康");
            context.setVariable("validMinutes", 5);

            String htmlContent = templateEngine.process("email/verification-code", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }
}