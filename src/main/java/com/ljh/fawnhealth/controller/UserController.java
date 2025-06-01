package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.user.UserLoginDTO;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.service.EmailService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.CharUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户模块
 * 提供用户的登录、注册等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private EmailService emailService;

    @Resource
    private UserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送邮件验证码
     *
     * @param email 接收邮箱
     * @return 发送结果
     */
    @GetMapping("/send")
    public BaseResponse<String> send(String email) {
        log.info("邮箱是：{}", email);
        // 生成6位验证码
        String emailCode = CharUtil.randomVerify();
        try {
            // 通过EmailService发送验证码邮件
            emailService.sendVerificationCode(email, emailCode);
            // 把验证码保存到Redis，5分钟过期
            stringRedisTemplate.opsForValue().set("email:code:" + email, emailCode, 5, TimeUnit.MINUTES);
            return ResultUtils.success("验证码发送成功");
        } catch (Exception e) {
            return ResultUtils.error(ErrorCode.EMAIL_VERIFICATION_FAILED);
        }
    }

    /**
     * 邮箱验证码登录
     *
     * @param userLoginDTO 登录相关DTO
     * @param request
     * @return UserLoginVO
     */
    @PostMapping("/login")
    public BaseResponse<UserLoginVO> loginWithEmail(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        // 从Redis取出验证码
        String redisCode = stringRedisTemplate.opsForValue().get("email:code:" + userLoginDTO.getEmail());

        if (redisCode == null) {
            return ResultUtils.error(ErrorCode.EMAIL_CODE);
        }

        if (!userLoginDTO.getCode().equals(redisCode)) {
            return ResultUtils.error(ErrorCode.EMAIL_VERIFICATION_FAILED);
        }

        // 获取客户端 IP 地址
        String clientIp = getClientIp(request);
        // 校验通过，调用登录逻辑
        UserLoginVO userLoginVO = userService.findUserByEmail(userLoginDTO.getEmail(),clientIp);

        return ResultUtils.success(userLoginVO);
    }

    /**
     * 退出登录
     *
     * @return 退出成功
     */
    @PostMapping("/logout")
    public BaseResponse<String> logout() {
        return ResultUtils.success("退出成功");
    }


    /**
     * 获取用户登录的ip地址
     *
     * @param request
     * @return 字符串
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理下，X-Forwarded-For 可能是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0];
        }
        return ip;
    }

}
