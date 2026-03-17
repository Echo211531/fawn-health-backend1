package com.zr.health.model.vo.user;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户公开信息视图对象（不包含敏感字段，如密码、手机号等）
 * 用于向前端展示用户的基本信息和健康相关数据
 */
@Data
public class UserInfoVO {

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别（0-未知，1-男，2-女，用于个性化展示）
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private Date birthday;

    /**
     * 用户身高
     */
    private BigDecimal height;

    /**
     * 当前体重
     */
    private BigDecimal weight;

    /**
     * 目标体重
     */
    private BigDecimal targetWeight;

    /**
     * 身体质量指数（BMI = 体重(kg) / 身高²(m)，用于健康状态评估）
     */
    private BigDecimal bmi;

    /**
     * 每日卡路里需求
     */
    private BigDecimal dailyCalories;

    /**
     * 是否为VIP用户（0-否，1-是，用于权限和功能区分）
     */
    private Integer isVip;

    /**
     * VIP会员过期时间
     */
    private Date vipExpireTime;

    /**
     * VIP会员等级（如1-普通VIP，2-高级VIP，用于差异化权益）
     */
    private Integer vipLevel;
}