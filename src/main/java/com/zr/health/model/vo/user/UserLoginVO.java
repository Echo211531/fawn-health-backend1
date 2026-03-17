package com.zr.health.model.vo.user;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 用户登录成功后返回给前端的视图对象（VO），
 * 包含了用户的基本信息、账户状态、VIP 信息、登录信息以及用于身份验证的 token 等。
 */
@Data
public class UserLoginVO {
    /**
     * 用户ID
     */
    private Long id;

    /**
     * 管理员登录用户名
     */
    private String userName;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证状态
     */
    private Integer emailVerified;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 性别:0未知,1男,2女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 身高(cm)
     */
    private BigDecimal height;

    /**
     * 体重(kg)
     */
    private BigDecimal weight;

    /**
     * 目标体重(kg)
     */
    private BigDecimal targetWeight;

    /**
     * BMI指数
     */
    private BigDecimal bmi;

    /**
     * 是否VIP:0否,1是
     */
    private Integer isVip;

    /**
     * VIP过期时间
     */
    private Date vipExpireTime;

    /**
     * 每日摄入的热量
     */
    private BigDecimal dailyCalories;

    /**
     * VIP等级
     */
    private Integer vipLevel;

    /**
     * 账号状态:0禁用,1正常
     */
    private Integer status;

    /**
     * 角色
     */
    private String role;

    /**
     * 最后登录时间
     */
    private Date lastLoginTime;

    /**
     * 最后登录IP
     */
    private String lastLoginIp;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * token
     */
    private String token;
}
