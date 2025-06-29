package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.config.JwtProperties;
import com.ljh.fawnhealth.constant.JwtClaimsConstant;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.enums.user.UserRole;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.JwtUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private JwtProperties jwtProperties; // 用于读取JWT配置（需配置此Bean）

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
     * @param email
     * @param loginIp
     * @return
     */
    @Override
    public UserLoginVO findUserByEmail(String email, String loginIp) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(queryWrapper);

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

        // 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims
        );

        // 构建返回对象
        UserLoginVO vo = new UserLoginVO();
        BeanUtils.copyProperties(user, vo);
        log.info("用户id是：{}",vo.getId());
        vo.setToken(token);

        return vo;
    }

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

        // 执行更新
        int rows = userMapper.updateById(updateUser);
        if (rows == 0) {
            throw new RuntimeException("更新用户信息失败");
        }
    }

    /** 计算BMI指数 */
    private BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal heightInMeters = height.divide(BigDecimal.valueOf(100));
        BigDecimal heightSquared = heightInMeters.multiply(heightInMeters);
        return weight.divide(heightSquared, 2, RoundingMode.HALF_UP);
    }

    /** 计算基础代谢率 */
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

    /** 获取默认基础代谢率 */
    private BigDecimal getDefaultBmr(Integer gender) {
        return gender == 1 ? new BigDecimal("1500") : new BigDecimal("1200");
    }

    /** 计算年龄 */
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

    /** 计算每日所需热量 */
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

}




