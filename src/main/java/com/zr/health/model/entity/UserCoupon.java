package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zr.health.model.enums.coupons.UserCouponStatus;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户领取优惠券的记录，是真正使用的优惠券信息
 * @TableName user_coupon
 */
@TableName(value ="user_coupon")
@Data
public class UserCoupon implements Serializable {
    /**
     * 用户券id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 优惠券的拥有者
     */
    private Long userId;

    /**
     * 优惠券模板id
     */
    private Long couponId;

    /**
     * 优惠券有效期开始时间
     */
    private Date termBeginTime;

    /**
     * 优惠券有效期结束时间
     */
    private Date termEndTime;

    /**
     * 优惠券使用时间（核销时间）
     */
    private Date usedTime;

    /**
     * 领取请求幂等键（抵御MQ重复投递）
     */
    private String requestId;

    /**
     * 优惠券状态，1：未使用，2：已使用，3：已失效
     */
    private UserCouponStatus status;

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