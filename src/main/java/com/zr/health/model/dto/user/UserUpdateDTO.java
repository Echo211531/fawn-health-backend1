package com.zr.health.model.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class UserUpdateDTO {
    /**
     * 用户ID
     */
    private Long id;

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
     * 目标天数
     */
    private Integer periodDays;
}