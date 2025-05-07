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
 * 优惠券使用记录表
 * @TableName coupon_usage_logs
 */
@TableName(value ="coupon_usage_logs")
@Data
public class CouponUsageLogs implements Serializable {
    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 使用时间
     */
    private Date useTime;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}