package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;


/**
 * 用户实体类，对应数据库中的 `user` 表，用于封装用户的相关信息。
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {

    /**
     * 用户ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 邮箱验证状态
     */
    private Integer emailVerified;

    /**
     * 密码(加密存储，可选)
     */
    private String password;

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
     * 角色
     */
    private String role;

    /**
     * 是否VIP:0否,1是
     */
    private Integer isVip;

    /**
     * VIP过期时间
     */
    private Date vipExpireTime;

    /**
     * VIP等级
     */
    private Integer vipLevel;

    /**
     * 账号状态:0禁用,1正常
     */
    private Integer status;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}