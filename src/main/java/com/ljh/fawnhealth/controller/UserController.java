package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.user.*;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.user.*;
import com.ljh.fawnhealth.service.EmailService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.CharUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
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

    /**
     * 分页查询用户列表（支持多条件筛选）
     * 适用于管理员后台查询用户数据
     *
     * @param queryDTO 分页及查询条件
     * @return 分页结果（包含用户列表及分页信息）
     */
    @PostMapping("/pageQuery")
    public BaseResponse<IPage<UserVO>> pageQueryUsers(@RequestBody UserPageQueryDTO queryDTO) {
        log.info("分页查询用户列表，参数：{}", queryDTO);
        // 校验分页参数合法性
        if (queryDTO.getPageNum() < 1 || queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码或每页条数无效（页码≥1，条数1-100）");
        }
        IPage<UserVO> pageResult = userService.pageQueryUsers(queryDTO);
        return ResultUtils.success(pageResult);
    }

    /**
     * 启用/禁用用户账号
     * 仅管理员可操作，支持批量处理（单个ID或多个ID用逗号分隔）
     *
     * @param userIds  用户ID列表（单个ID或多个ID用逗号分隔，如"1,2,3"）
     * @param status   目标状态（0-禁用，1-启用）
     * @param id  请求对象
     * @return 操作结果
     */
    @PostMapping("/updateStatus")
    public BaseResponse<String> updateUserStatus(@RequestParam String userIds, @RequestParam Integer status, @RequestParam Long id) {
        log.info("修改用户账号状态，用户ID：{}，目标状态：{}", userIds, status);
        userService.updateUserStatus(userIds, status, id);
        return ResultUtils.success("用户状态修改成功");
    }

    /**
     * 添加管理员账号
     * 仅超级管理员可操作，默认角色为admin，密码默认123456（MD5加密）
     *
     * @param adminAddDTO 管理员信息（用户名、性别、邮箱）
     * @return 添加结果
     */
    @PostMapping("/addAdmin")
    public BaseResponse<String> addAdmin(@Validated @RequestBody AdminAddDTO adminAddDTO,Long id) {
        log.info("添加管理员账号，参数：{}", adminAddDTO);
        userService.addAdmin(adminAddDTO,id);
        return ResultUtils.success("管理员账号创建成功，默认密码：123456");
    }

    /**
     * 管理员修改个人信息接口
     *
     * @param adminId 管理员ID（路径参数，用于定位修改对象）
     * @param adminUpdateDTO 管理员信息更新参数
     * @return 更新后的管理员信息
     */
    @PostMapping("/admin/updateInfo/{adminId}")
    public BaseResponse<User> updateAdminInfo(
            @PathVariable Long adminId,
            @RequestBody AdminUpdateDTO adminUpdateDTO) {

        log.info("管理员修改个人信息，管理员ID:{}，更新内容:{}", adminId, adminUpdateDTO);

        User updatedAdmin = userService.updateAdminInfo(adminId, adminUpdateDTO);
        return ResultUtils.success(updatedAdmin);
    }

}
