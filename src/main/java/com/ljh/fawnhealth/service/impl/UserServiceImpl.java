package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.config.JwtProperties;
import com.ljh.fawnhealth.constant.JwtClaimsConstant;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.UserLoginLogMapper;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.mapper.UserWeightHistoryMapper;
import com.ljh.fawnhealth.model.dto.user.AdminAddDTO;
import com.ljh.fawnhealth.model.dto.user.AdminUpdateDTO;
import com.ljh.fawnhealth.model.dto.user.UserPageQueryDTO;
import com.ljh.fawnhealth.model.dto.user.UserUpdateDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.entity.UserLoginLog;
import com.ljh.fawnhealth.model.entity.UserWeightHistory;
import com.ljh.fawnhealth.model.enums.user.UserRole;
import com.ljh.fawnhealth.model.vo.user.*;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.JwtUtil;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ljh.fawnhealth.constant.UserConstant.USER_LOGIN_STATE;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private UserWeightHistoryMapper userWeightHistoryMapper;

    @Resource
    private JwtProperties jwtProperties; // 用于读取JWT配置（需配置此Bean）

    @Resource
    private UserLoginLogMapper userLoginLogMapper;

    // 基础代谢计算系数
    private static final BigDecimal MALE_FACTOR = new BigDecimal("10");
    private static final BigDecimal FEMALE_FACTOR = new BigDecimal("9.25");
    private static final BigDecimal HEIGHT_FACTOR = new BigDecimal("6.25");
    private static final BigDecimal AGE_FACTOR_MALE = new BigDecimal("5");
    private static final BigDecimal AGE_FACTOR_FEMALE = new BigDecimal("4.92");

    // 热量计算常量
    private static final BigDecimal CALORIES_PER_KG = new BigDecimal("7700");
    private static final BigDecimal DEFAULT_ACTIVITY_FACTOR = new BigDecimal("1.55");


    /**
     * 邮箱验证码登录
     *
     * @param email
     * @param loginIp
     * @return
     */
    @Override
    public UserLoginVO findUserByEmail(String email, String loginIp, HttpServletRequest request) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(queryWrapper);

        // 登录状态默认设为成功
        int loginStatus = 1;
        String failReason = null;

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setEmailVerified(0);
            user.setNickname("小鹿友友" + UUID.randomUUID().toString().substring(0, 6));
            user.setStatus(1);
            user.setIsVip(0);
            user.setGender(0);
            user.setRole(UserRole.USER.getDescription());
            user.setAvatar("http://fawn-health.oss-cn-chengdu.aliyuncs.com/fawn-health-userAvatar.png");
            user.setCreateTime(new Date());
        }

        try {
            if (user.getStatus() == 0) {
                // 账号被封禁时设置登录失败状态和原因
                loginStatus = 0;
                failReason = "账号被封禁，请联系管理员";
                throw new BusinessException(ErrorCode.PARAMS_ERROR, failReason);
            }

            // 每次登录都更新以下字段
            user.setLastLoginTime(new Date());
            user.setLastLoginIp(loginIp);
            user.setUpdateTime(new Date());

            // 插入或更新数据库
            if (user.getId() == null) {
                userMapper.insert(user);
            } else {
                userMapper.updateById(user);
            }
        } catch (BusinessException e) {
            // 如果是已知业务异常，保持之前设置的失败原因
            if (failReason == null) {
                loginStatus = 0;
                failReason = e.getMessage();
            }
            throw e; // 重新抛出异常，不影响原有业务流程
        } catch (Exception e) {
            // 处理其他未知异常
            loginStatus = 0;
            failReason = "登录过程发生未知错误: " + e.getMessage();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, failReason);
        } finally {
            // 无论登录成功与否，都记录登录日志
            UserLoginLog loginLog = new UserLoginLog();
            loginLog.setUserId(user.getId());
            loginLog.setLoginTime(new Date());
            loginLog.setLoginStatus(loginStatus);
            loginLog.setFailReason(failReason);
            // 插入登录日志
            userLoginLogMapper.insert(loginLog);
        }

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );
        // 记录用户的登录态，存入用户信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);

        // 构建返回对象
        UserLoginVO vo = new UserLoginVO();
        BeanUtils.copyProperties(user, vo);
        log.info("用户id是：{}", vo.getId());
        vo.setToken(token);

        return vo;
    }

    /**
     * 管理员登录
     *
     * @param username
     * @param password
     * @return
     */
    @Override
    public UserLoginVO findAdminByEmail(String username, String password) {
        // 参数校验
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户名或密码不能为空");
        }

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUserName, username)
                .or()
                .eq(User::getEmail, username); // 支持用户名或邮箱登录
        User user = userMapper.selectOne(queryWrapper);

        // 验证用户是否存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名或密码错误");
        }

        // 验证是否为管理员角色
        if (!"admin".equals(user.getRole()) && !"super_admin".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "没有管理员权限，无法登录");
        }

        // 验证账号状态
        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已被禁用，请联系超级管理员");
        }

        // 对前端传过来的明文密码进行md5加密
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());

        // 验证密码
        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 更新最后登录信息
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginTime(new Date());
        userMapper.updateById(updateUser);
        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );
        UserLoginVO loginVO = new UserLoginVO();
        BeanUtils.copyProperties(user, loginVO);
        log.info("用户id是：{}",loginVO.getId());
        loginVO.setToken(token);

        return loginVO;
    }

    /**
     * 更新用户体重和目标体重信息
     *
     * @param userId 用户ID
     * @param weight 体重(kg)
     * @param targetWeight 目标体重(kg)
     * @return 更新是否成功
     */
    @Override
    public void updateWeightInfo(Long userId, BigDecimal weight, BigDecimal targetWeight, Integer periodDays) {
        // 查询用户信息
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 计算BMI
        BigDecimal bmi = calculateBmi(weight, user.getHeight());
        log.info("用户{}的BMI计算结果: {}", userId, bmi);

        // 计算基础代谢率(BMR)，含空值处理
        BigDecimal bmr = calculateBmr(
                userId,
                weight,
                user.getHeight(),
                user.getGender(),
                user.getBirthday()
        );
        bmr = bmr != null ? bmr : getDefaultBmr(user.getGender());
        log.info("用户{}基础代谢率: {}大卡", userId, bmr);

        // 计算每日总消耗热量(TDEE)
        BigDecimal tdee = bmr.multiply(DEFAULT_ACTIVITY_FACTOR);

        // 计算每日所需热量
        BigDecimal dailyCalories = calculateDailyCalories(
                weight,
                targetWeight,
                periodDays,
                tdee
        );

        // 更新用户信息
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setWeight(weight);
        updateUser.setTargetWeight(targetWeight);
        updateUser.setBmi(bmi);
        updateUser.setDailyCalories(dailyCalories);

        // 记录体重历史
        UserWeightHistory history = new UserWeightHistory();
        history.setUserId(userId);
        history.setWeight(weight);
        history.setRecordDate(new Date());
        userWeightHistoryMapper.insert(history);

        // 执行更新
        int rows = userMapper.updateById(updateUser);
        if (rows == 0) {
            throw new RuntimeException("更新用户信息失败");
        }
    }

    /**
     * 计算BMI指数
     *
     * @param weight
     * @param height
     * @return
     */
    private BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightInMeters = height.divide(BigDecimal.valueOf(100));
        BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);
        return weight.divide(heightSquared, 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算基础代谢率
     *
     * @param userId
     * @param weight
     * @param height
     * @param gender
     * @param birthday
     * @return
     */
    private BigDecimal calculateBmr(Long userId,BigDecimal weight, BigDecimal height, Integer gender, Date birthday) {
        // 参数有效性校验
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("用户{}体重参数无效: {}", userId, weight);
            return null;
        }
        if (height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("用户{}身高参数无效: {}", userId, height);
            return null;
        }
        if (gender == null || (gender != 1 && gender != 2)) {
            log.error("用户{}性别参数无效: {}", userId, gender);
            return null;
        }
        if (birthday == null) {
            log.error("用户{}生日参数无效: {}", userId, birthday);
            return null;
        }

        // 计算年龄
        int age = calculateAge(birthday);

        // 基础代谢率计算
        BigDecimal weightFactor = gender == 1 ? MALE_FACTOR : FEMALE_FACTOR;
        BigDecimal ageFactor = gender == 1 ? AGE_FACTOR_MALE : AGE_FACTOR_FEMALE;

        BigDecimal bmr = weight.multiply(weightFactor)
                .add(height.multiply(HEIGHT_FACTOR))
                .subtract(new BigDecimal(age).multiply(ageFactor));

        if (gender == 2) { // 女性需要减161
            bmr = bmr.subtract(new BigDecimal("161"));
        }

        return bmr.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * 获取默认基础代谢率
     *
     * @param gender
     * @return
     */
    private BigDecimal getDefaultBmr(Integer gender) {
        return gender == 1 ? new BigDecimal("1500") : new BigDecimal("1200");
    }

    /**
     * 计算年龄
     * @param birthday
     * @return
     */
    private int calculateAge(Date birthday) {
        Calendar birth = Calendar.getInstance();
        birth.setTime(birthday);
        Calendar now = Calendar.getInstance();
        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.MONTH) < birth.get(Calendar.MONTH) ||
                (now.get(Calendar.MONTH) == birth.get(Calendar.MONTH) &&
                        now.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }
        return age;
    }

    /**
     * 计算每日所需热量
     *
     * @param currentWeight
     * @param targetWeight
     * @param periodDays
     * @param tdee
     * @return
     */
    private BigDecimal calculateDailyCalories(
            BigDecimal currentWeight,
            BigDecimal targetWeight,
            Integer periodDays,
            BigDecimal tdee) {

        if (periodDays == null || periodDays <= 0) {
            return tdee; // 无周期时返回维持体重的热量
        }

        // 计算体重差值
        BigDecimal weightDifference = targetWeight.subtract(currentWeight);

        // 计算总额外热量
        BigDecimal totalExtraCalories = weightDifference.multiply(CALORIES_PER_KG);

        // 计算每日额外热量
        BigDecimal dailyExtraCalories = totalExtraCalories.divide(
                new BigDecimal(periodDays), 0, RoundingMode.HALF_UP
        );

        // 增重/减重计算
        if (weightDifference.compareTo(BigDecimal.ZERO) > 0) {
            return tdee.add(dailyExtraCalories);
        } else {
            return tdee.subtract(dailyExtraCalories.abs());
        }
    }

    /**
     * 获取当前用户
     *
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        BaseContext.setCurrentId(userId);

        return currentUser;
    }

    /**
     * 更新用户信息（支持部分字段更新）
     *
     * @param userId        用户ID（当前登录用户）
     * @param userUpdateDTO 待更新的用户信息DTO
     * @return 更新后的用户实体
     */
    @Override
    public User updateUserInfo(Long userId, UserUpdateDTO userUpdateDTO) {
        // 1. 校验用户是否存在
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOTFOUND);
        }

        // 标记是否需要重新计算每日热量
        boolean needRecalculateCalories = false;
        // 记录原始体重和目标体重（用于对比是否变化）
        BigDecimal originalWeight = user.getWeight();
        BigDecimal originalTargetWeight = user.getTargetWeight();

        // 2. 处理头像URL
        if (userUpdateDTO.getAvatar() != null && !userUpdateDTO.getAvatar().trim().isEmpty()) {
            user.setAvatar(userUpdateDTO.getAvatar());
        }

        // 3. 处理昵称
        if (userUpdateDTO.getNickname() != null && !userUpdateDTO.getNickname().trim().isEmpty()) {
            if (userUpdateDTO.getNickname().length() > 50) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度不能超过50字符");
            }
            user.setNickname(userUpdateDTO.getNickname());
        }

        // 4. 处理生日
        if (userUpdateDTO.getBirthday() != null) {
            user.setBirthday(userUpdateDTO.getBirthday());
        }

        // 5. 处理身高
        boolean isHeightUpdated = false;
        if (userUpdateDTO.getHeight() != null) {
            if (userUpdateDTO.getHeight().compareTo(new BigDecimal("50")) < 0
                    || userUpdateDTO.getHeight().compareTo(new BigDecimal("250")) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "身高范围应在50-250cm之间");
            }
            user.setHeight(userUpdateDTO.getHeight());
            isHeightUpdated = true;
        }

        // 6. 处理体重（变化时需要重新计算热量）
        boolean isWeightUpdated = false;
        if (userUpdateDTO.getWeight() != null) {
            if (userUpdateDTO.getWeight().compareTo(new BigDecimal("10")) < 0
                    || userUpdateDTO.getWeight().compareTo(new BigDecimal("300")) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "体重范围应在10-300kg之间");
            }
            // 对比原始体重，判断是否变化
            if (!userUpdateDTO.getWeight().equals(originalWeight)) {
                user.setWeight(userUpdateDTO.getWeight());
                isWeightUpdated = true;
                needRecalculateCalories = true; // 体重变化，需要重新计算热量
            }
        }

        // 7. 处理目标体重（变化时需要重新计算热量）
        if (userUpdateDTO.getTargetWeight() != null) {
            if (user.getWeight() != null && userUpdateDTO.getTargetWeight().compareTo(new BigDecimal("5")) < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标体重不能低于5kg");
            }
            // 对比原始目标体重，判断是否变化
            if (!userUpdateDTO.getTargetWeight().equals(originalTargetWeight)) {
                user.setTargetWeight(userUpdateDTO.getTargetWeight());
                needRecalculateCalories = true; // 目标体重变化，需要重新计算热量
            }
        }

        // 8. 处理性别
        if (userUpdateDTO.getGender() != null) {
            if (userUpdateDTO.getGender() < 0 || userUpdateDTO.getGender() > 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别参数无效（0-未知，1-男，2-女）");
            }
            user.setGender(userUpdateDTO.getGender());
        }

        // 9. 重新计算BMI（身高或体重变化时）
        if (isWeightUpdated || isHeightUpdated) {
            BigDecimal newBmi = calculateBmi(user.getWeight(), user.getHeight());
            user.setBmi(newBmi);
        }

        // 10. 重新计算每日热量（体重或目标体重变化时）
        if (needRecalculateCalories) {
            // 计算基础代谢率(BMR)
            BigDecimal bmr = calculateBmr(
                    userId,
                    user.getWeight(),
                    user.getHeight(),
                    user.getGender(),
                    user.getBirthday()
            );
            bmr = bmr != null ? bmr : getDefaultBmr(user.getGender());

            // 计算每日总消耗热量(TDEE)
            BigDecimal tdee = bmr.multiply(DEFAULT_ACTIVITY_FACTOR);

            // 关键修改：使用DTO中的periodDays（目标天数）计算每日热量
            Integer periodDays = userUpdateDTO.getPeriodDays(); // 从DTO获取目标天数
            BigDecimal dailyCalories = calculateDailyCalories(
                    user.getWeight(),       // 当前体重（已更新后的值）
                    user.getTargetWeight(), // 目标体重（已更新后的值）
                    periodDays,             // DTO中的目标天数
                    tdee
            );

            user.setDailyCalories(dailyCalories);
            log.info("用户{}因体重/目标体重变化，基于{}天周期重新计算每日热量: {}大卡",
                    userId, periodDays, dailyCalories);
        }

        // 11. 更新时间戳
        user.setUpdateTime(new Date());

        // 12. 执行数据库更新
        int rows = userMapper.updateById(user);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户信息失败");
        }

        // 13. 返回更新后的用户信息
        return userMapper.selectById(userId);
    }

    /**
     * 批量获取用户信息
     *
     * @param userIds
     */
    @Override
    public Map<Long, User> getUserMapByIds(Set<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    /**
     * 实现用户新增数据统计逻辑
     */
    @Override
    public UserNewStatisticsVO getNewUsersStatistics() {
        // 获取当前时间（上海时区）
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        // 1. 总用户数：所有有效注册用户的总数
        long userTotal = countTotalUsers ();

        // 1. 今日新增用户
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime todayEnd = now;
        long todayNewUsers = countNewUsers(todayStart, todayEnd);

        // 2. 昨日新增用户
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = yesterdayStart.with(LocalTime.MAX);
        long yesterdayNewUsers = countNewUsers(yesterdayStart, yesterdayEnd);

        // 3. 本月新增用户
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime monthEnd = now;
        long monthNewUsers = countNewUsers(monthStart, monthEnd);

        // 4. 计算日环比增长率
        BigDecimal dayOnDayRate = calculateDayOnDayRate(todayNewUsers, yesterdayNewUsers);

        // 封装结果
        UserNewStatisticsVO statisticsVO = new UserNewStatisticsVO();
        statisticsVO.setTodayNewUsers(todayNewUsers);
        statisticsVO.setYesterdayNewUsers(yesterdayNewUsers);
        statisticsVO.setMonthNewUsers(monthNewUsers);
        statisticsVO.setDayOnDayRate(dayOnDayRate);
        statisticsVO.setUserTotal(userTotal);
        statisticsVO.setStatisticTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        return statisticsVO;
    }

    /**
     * 统计总用户数（所有有效注册用户）
     * @return
     */
    private long countTotalUsers () {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        return userMapper.selectCount (queryWrapper);
    }

    @Override
    public UserLoginStatisticsVO getLoginStatistics() {
        // 使用上海时区确保时间准确性
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));

        long userTotal = countTotalLoginCount();
        // 1. 今日登录用户数：今日00:00:00至当前时间
        LocalDateTime todayStart = now.with(LocalTime.MIN);
        LocalDateTime todayEnd = now;
        long todayLoginUsers = countLoginUsers(todayStart, todayEnd);

        // 2. 昨日登录用户数：昨日00:00:00至昨日23:59:59
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = yesterdayStart.with(LocalTime.MAX);
        long yesterdayLoginUsers = countLoginUsers(yesterdayStart, yesterdayEnd);

        // 3. 本月登录用户数：本月1日00:00:00至当前时间（新增逻辑）
        LocalDateTime monthStart = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime monthEnd = now;
        long monthLoginUsers = countLoginUsers(monthStart, monthEnd);

        // 4. 计算日环比增长率（今日较昨日）
        BigDecimal dayOnDayRate = calculateDayOnDayRate(todayLoginUsers, yesterdayLoginUsers);

        // 封装结果（包含新增的本月登录用户数）
        UserLoginStatisticsVO statisticsVO = new UserLoginStatisticsVO();
        statisticsVO.setTodayLoginUsers(todayLoginUsers);
        statisticsVO.setYesterdayLoginUsers(yesterdayLoginUsers);
        statisticsVO.setMonthLoginUsers(monthLoginUsers);  // 设置新增字段
        statisticsVO.setDayOnDayRate(dayOnDayRate);
        statisticsVO.setUserTotal(userTotal); // 总的用户登录成功数量
        statisticsVO.setStatisticTime(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));

        return statisticsVO;
    }

    /**
     * 统计总登录数量（所有成功登录的记录总数）
     */
    private long countTotalLoginCount() {
        LambdaQueryWrapper<UserLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserLoginLog::getLoginStatus, 1);  // 只统计成功登录的记录
        return userLoginLogMapper.selectCount(queryWrapper);
    }

    /**
     * 分页查询用户列表（支持多条件筛选）
     * 适用于管理员后台查询用户数据
     *
     * @param queryDTO 分页及查询条件
     * @return 分页结果（包含用户列表及分页信息）
     */
    @Override
    public IPage<UserVO> pageQueryUsers(UserPageQueryDTO queryDTO) {
        // 1. 创建分页对象
        Page<User> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();

        // 用户ID精确查询
        if (queryDTO.getUserId() != null) {
            queryWrapper.eq(User::getId, queryDTO.getUserId());
        }

        // 邮箱模糊查询（支持部分匹配，如输入"test"匹配"test@example.com"）
        String email = queryDTO.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            queryWrapper.like(User::getEmail, email.trim());
        }

        // 性别精确查询（0/1/2）
        if (queryDTO.getGender() != null) {
            // 校验性别参数有效性
            if (queryDTO.getGender() < 0 || queryDTO.getGender() > 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别参数无效（0-未知，1-男，2-女）");
            }
            queryWrapper.eq(User::getGender, queryDTO.getGender());
        }

        // 是否VIP精确查询（0/1）
        if (queryDTO.getIsVip() != null) {
            // 校验VIP参数有效性
            if (queryDTO.getIsVip() != 0 && queryDTO.getIsVip() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP参数无效（0-否，1-是）");
            }
            queryWrapper.eq(User::getIsVip, queryDTO.getIsVip());
        }

        // 账号状态精确查询（0/1）
        if (queryDTO.getStatus() != null) {
            // 校验状态参数有效性
            if (queryDTO.getStatus() != 0 && queryDTO.getStatus() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数无效（0-禁用，1-正常）");
            }
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }

        // 排序：默认按创建时间降序（最新注册用户在前）
        queryWrapper.orderByDesc(User::getCreateTime);

        // 3. 执行分页查询
        Page<User> userPage = this.page(page, queryWrapper);

        // 4. 转换为VO（脱敏处理，只返回必要字段）
        List<UserVO> userInfoVOList = userPage.getRecords().stream()
                .map(user -> {
                    UserVO vo = new UserVO();
                    BeanUtils.copyProperties(user, vo); // 复制同名字段（如id、nickname、gender等）
                    // 如需额外处理（如隐藏邮箱部分字符），可在此处添加
                    // vo.setEmail(maskEmail(user.getEmail()));
                    return vo;
                })
                .collect(Collectors.toList());

        // 5. 封装分页结果
        IPage<UserVO> resultPage = new Page<>();
        resultPage.setRecords(userInfoVOList);
        resultPage.setTotal(userPage.getTotal()); // 总条数
        resultPage.setCurrent(userPage.getCurrent()); // 当前页码
        resultPage.setSize(userPage.getSize()); // 每页条数
        resultPage.setPages(userPage.getPages()); // 总页数

        return resultPage;
    }

    /**
     * 启用/禁用用户账号
     * 仅管理员可操作，支持批量处理（单个ID或多个ID用逗号分隔）
     *
     * @param userIds 用户ID列表（单个ID或多个ID用逗号分隔，如"1,2,3"）
     * @param status  目标状态（0-禁用，1-启用）
     * @param adminId 请求对象
     * @return 操作结果
     */
    @Transactional
    @Override
    public void updateUserStatus(String userIds, Integer status, Long adminId) {
        // 1. 权限校验（仅管理员可操作）
        User loginUser = userMapper.selectById(adminId);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        if (!"admin".equals(loginUser.getRole()) && !"super_admin".equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 2. 参数校验（用户ID）
        if (userIds == null || userIds.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        String[] userIdArray = userIds.split(",");
        if (userIdArray.length == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID格式错误（示例：1 或 1,2,3）");
        }

        // 3. 参数校验（状态值）
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数无效（0-禁用，1-启用）");
        }

        // 4. 转换用户ID为Long类型并校验格式
        List<Long> userIdList;
        try {
            userIdList = Arrays.stream(userIdArray)
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须为数字（示例：1 或 1,2,3）");
        }

        // 5. 通过 Mapper 校验用户是否存在
        List<User> existUsers = userMapper.selectBatchIds(userIdList);
        if (existUsers.size() != userIdList.size()) {
            List<Long> existIds = existUsers.stream().map(User::getId).collect(Collectors.toList());
            List<Long> notExistIds = userIdList.stream()
                    .filter(id -> !existIds.contains(id))
                    .collect(Collectors.toList());
            throw new BusinessException(ErrorCode.USER_NOTFOUND, "以下用户不存在：" + notExistIds);
        }

        // 6. 通过 Mapper 执行批量更新（替代 this.update）
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(User::getId, userIdList)
                .set(User::getStatus, status);

        int updateCount = userMapper.update(null, updateWrapper);
        if (updateCount <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户状态失败");
        }
    }

    /**
     * 添加管理员账号
     * 仅超级管理员可操作，默认角色为admin，密码默认123456（MD5加密）
     *
     * @param adminAddDTO 管理员信息（用户名、性别、邮箱）
     * @return 添加结果
     */
    @Override
    public void addAdmin(AdminAddDTO adminAddDTO, Long id) {
        // 1. 权限校验：仅超级管理员可添加管理员
        User loginUser = userMapper.selectById(id);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!"super_admin".equals(loginUser.getRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作，仅超级管理员可添加管理员");
        }

        // 2. 校验邮箱唯一性（数据库唯一索引也会限制，这里提前校验并返回友好提示）
        User existUser = userMapper.selectOne(
                new QueryWrapper<User>()
                        .lambda()
                        .eq(User::getEmail, adminAddDTO.getEmail())
        );
        if (existUser != null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
        }

        // 3. 构建用户实体（填充默认值）
        User adminUser = new User();
        adminUser.setUserName(adminAddDTO.getUsername());
        adminUser.setGender(adminAddDTO.getGender());
        adminUser.setEmail(adminAddDTO.getEmail());

        // 4. 处理密码：默认123456，MD5加密
        String defaultPassword = "123456";
        String encryptedPassword = DigestUtils.md5DigestAsHex(defaultPassword.getBytes(StandardCharsets.UTF_8));
        adminUser.setPassword(encryptedPassword);

        // 5. 设置默认值
        adminUser.setRole("admin"); // 角色默认为管理员
        adminUser.setNickname("管理员");
        adminUser.setIsVip(0); // 非VIP
        adminUser.setAvatar("http://fawn-health.oss-cn-chengdu.aliyuncs.com/fawn-health-userAvatar.png"); // 默认头像
        adminUser.setEmailVerified(0); // 邮箱未验证
        adminUser.setStatus(1); // 账号默认启用

        // 6. 插入数据库
        int insert = userMapper.insert(adminUser);
        if (insert != 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "管理员账号创建失败");
        }
    }

    /**
     * 管理员修改个人信息接口
     *
     * @param adminId        管理员ID（路径参数，用于定位修改对象）
     * @param adminUpdateDTO 管理员信息更新参数
     * @return 更新后的管理员信息
     */
    @Override
    public User updateAdminInfo(Long adminId, AdminUpdateDTO adminUpdateDTO) {
        // 1. 基础校验：ID合法性
        if (adminId == null || adminId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "管理员ID无效");
        }

        // 2. 校验管理员是否存在
        User admin = this.getById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.USER_NOTFOUND, "管理员不存在");
        }

        // 3. 权限校验：是否为管理员角色
        if (!"admin".equals(admin.getRole()) && !"super_admin".equals(admin.getRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前账号无管理员权限");
        }

        // 4. 字段校验与赋值
        // 4.1 昵称校验（1-50字符）
        if (adminUpdateDTO.getNickname() != null) {
            String nickname = adminUpdateDTO.getNickname().trim();
            if (nickname.isEmpty() || nickname.length() > 50) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称长度必须为1-50字符");
            }
            admin.setNickname(nickname);
        }

        // 4.2 头像URL校验（非空时校验格式）
        if (adminUpdateDTO.getAvatar() != null) {
            String avatar = adminUpdateDTO.getAvatar().trim();
            if (!avatar.isEmpty()
                    && !avatar.startsWith("http://")
                    && !avatar.startsWith("https://")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像URL格式必须以http://或https://开头");
            }
            admin.setAvatar(avatar);
        }

        // 4.3 性别校验（必须为0/1/2）
        if (adminUpdateDTO.getGender() != null) {
            if (adminUpdateDTO.getGender() < 0 || adminUpdateDTO.getGender() > 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别必须为0（未知）、1（男）、2（女）");
            }
            admin.setGender(adminUpdateDTO.getGender());
        }

        admin.setBirthday(adminUpdateDTO.getBirthday());

        // 4.4 用户名校验（4-20位字母数字）
        if (adminUpdateDTO.getUsername() != null) {
            String username = adminUpdateDTO.getUsername().trim();
            if (username.isEmpty() || username.length() < 4 || username.length() > 20) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度必须为4-20字符");
            }
            if (!username.matches("^[a-zA-Z0-9]+$")) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名只能包含字母和数字");
            }
            // 额外校验用户名唯一性（避免重复）
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserName, username)
                    .ne(User::getId, adminId); // 排除当前管理员自身
            if (this.count(queryWrapper) > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名已被占用");
            }
            admin.setUserName(username);
        }

        // 5. 执行更新
        boolean updateSuccess = this.updateById(admin);
        if (!updateSuccess) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改个人信息失败，请重试");
        }

        // 6. 返回更新后的管理员信息
        return this.getById(adminId);
    }

    /**
     * 获取用户性别分布统计数据
     *
     * @return 性别分布统计VO
     */
    @Override
    public UserGenderStatisticsVO getGenderDistributionStatistics() {
        // 1. 查询各性别用户数量（0-未知，1-男，2-女）
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("gender", "COUNT(*) as count");
        queryWrapper.groupBy("gender");
        List<Map<String, Object>> genderMaps = userMapper.selectMaps(queryWrapper);

        // 2. 初始化统计数据（默认值为0）
        int unknownCount = 0;
        int maleCount = 0;
        int femaleCount = 0;

        // 3. 解析查询结果
        for (Map<String, Object> map : genderMaps) {
            Integer gender = (Integer) map.get("gender");
            Integer count = Integer.parseInt(map.get("count").toString());

            if (gender == 0) {
                unknownCount = count;
            } else if (gender == 1) {
                maleCount = count;
            } else if (gender == 2) {
                femaleCount = count;
            }
        }

        // 4. 计算总用户数
        int totalUserCount = unknownCount + maleCount + femaleCount;

        // 5. 封装VO对象
        UserGenderStatisticsVO statisticsVO = new UserGenderStatisticsVO();
        statisticsVO.setGenderLabels(Arrays.asList("未知", "男", "女"));
        statisticsVO.setGenderCounts(Arrays.asList(unknownCount, maleCount, femaleCount));
        statisticsVO.setTotalUserCount(totalUserCount);

        return statisticsVO;
    }

    /**
     * 邮箱脱敏（如：test@example.com → t***@example.com）
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String prefix = parts[0];
        if (prefix.length() <= 1) {
            return prefix + "***@" + parts[1];
        }
        return prefix.charAt(0) + "***@" + parts[1];
    }

    /**
     * 统计指定时间段内的登录用户数
     */
    private long countLoginUsers(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        // 统计最后登录时间在指定时间段内的用户
        queryWrapper.between(User::getLastLoginTime, startTime, endTime);
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 统计指定时间段内的新增用户数
     *
     * @param startTime
     * @param endTime
     * @return
     */
    private long countNewUsers(LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.between(User::getCreateTime, startTime, endTime);
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 计算日环比增长率（今日较昨日）
     *
     * @param todayNewUsers
     * @param yesterdayNewUsers
     * @return
     */
    private BigDecimal calculateDayOnDayRate(long todayNewUsers, long yesterdayNewUsers) {
        if (yesterdayNewUsers <= 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(todayNewUsers - yesterdayNewUsers)
                .divide(new BigDecimal(yesterdayNewUsers), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100));
    }

}




