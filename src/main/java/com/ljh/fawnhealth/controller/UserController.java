package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.user.UserLoginDTO;
import com.ljh.fawnhealth.model.dto.user.UserUpdateDTO;
import com.ljh.fawnhealth.model.dto.user.WeightDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.UserInfoVO;
import com.ljh.fawnhealth.model.vo.user.UserLoginStatisticsVO;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.model.vo.user.UserNewStatisticsVO;
import com.ljh.fawnhealth.service.EmailService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.CharUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
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
        UserLoginVO userLoginVO = userService.findUserByEmail(userLoginDTO.getEmail(),clientIp,request);
        log.info("userLoginVO:{}", userLoginVO);
        return ResultUtils.success(userLoginVO);
    }

    /**
     * 管理员登录
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/loginAdmin")
    public BaseResponse<UserLoginVO> loginWithUserNameAdmin(String username, String password) {
        UserLoginVO userLoginVO = userService.findAdminByEmail(username,password);
        log.info("userLoginVO:{}", userLoginVO);
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
     * 设置用户体重和目标体重
     *
     * @param request      HTTP请求
     * @param weightDTO    包含体重、目标体重和周期的DTO
     * @return 更新结果
     */
    @PostMapping("/weight")
    public BaseResponse<String> setWeightAndTargetWeight(HttpServletRequest request, @RequestBody WeightDTO weightDTO) {
        Long userId = weightDTO.getUserId();
        if (userId == null) {
            return ResultUtils.error(ErrorCode.USER_NOTFOUND);
        }

        // 调用服务层更新用户体重信息（传递周期参数）
        userService.updateWeightInfo(userId, weightDTO.getWeight(), weightDTO.getTargetWeight(), weightDTO.getPeriodDays());
        return ResultUtils.success("体重信息更新成功");
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

    /**
     * 获取当前用户信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<User> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(user);
    }

    /**
     * 修改用户信息接口
     * 支持部分字段更新（昵称、头像、生日、身高、体重、目标体重）
     *
     * @param userUpdateDTO
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<User> updateUserInfo(
            @RequestBody UserUpdateDTO userUpdateDTO,
            HttpServletRequest request) {
        // 1. 优先从上下文获取当前登录用户ID
        Long userId = BaseContext.getCurrentId();

        // 2. 若上下文无ID，尝试从DTO中获取
        if (userId == null) {
            userId = userUpdateDTO.getId();
            // 3. 双重校验，确保userId不为空
            if (userId == null) {
                log.error("用户ID为空，无法更新信息");
            }
            log.warn("从DTO中获取用户ID：{}（建议优先使用登录上下文）", userId);
        }

        log.info("正在修改的userId:{}", userId);

        // 4. 调用服务层更新用户信息
        User updatedUser = userService.updateUserInfo(userId, userUpdateDTO);
        return ResultUtils.success(updatedUser);

    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息（脱敏处理）
     */
    @GetMapping("/info/{userId}")
    public BaseResponse<UserInfoVO> getUserInfoById(@PathVariable Long userId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID无效");
        }

        // 查询用户信息
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOTFOUND);
        }

        // 转换为 UserInfoVO（仅暴露需要的字段）
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(user, userInfoVO); // 自动匹配同名字段

        return ResultUtils.success(userInfoVO);
    }

    /**
     * 用户新增数据统计
     * 获取今日、昨日、本月新增用户数及日环比增长率
     *
     * @return 统计结果VO
     */
    @GetMapping("/statisticsNewUsers")
    public BaseResponse<UserNewStatisticsVO> getNewUsersStatistics() {
        UserNewStatisticsVO newUsersStatistics = userService.getNewUsersStatistics();
        return ResultUtils.success(newUsersStatistics);
    }

    /**
     * 用户登录数据统计
     * 获取今日、昨日登录用户数及日环比增长率
     *
     * @return 统计结果VO
     */
    @GetMapping("/statisticsLogin")
    public BaseResponse<UserLoginStatisticsVO> getLoginStatistics() {
        UserLoginStatisticsVO statisticsVO = userService.getLoginStatistics();
        return ResultUtils.success(statisticsVO);
    }

}
