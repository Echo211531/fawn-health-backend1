package com.ljh.fawnhealth.model.vo.coupons;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户优惠券视图对象
 * 封装了用户持有的优惠券信息，用于前端展示
 */
@Data
public class UserCouponsVO {
    /**
     * 优惠券记录ID
     */
    private Long id;

    /**
     * 关联的优惠券模板ID
     */
    private Long couponId;

    /**
     * 优惠券名称
     */
    private String couponName;

    /**
     * 优惠券状态：
     * 0-未使用，1-已使用，2-已过期
     */
    private Integer status;

    /**
     * 有效期开始时间
     */
    private Date termBeginTime;

    /**
     * 有效期结束时间
     */
    private Date termEndTime;

    /**
     * 使用时间（状态为已使用时有效）
     */
    private Date usedTime;

    /**
     * 折扣类型：
     * 1-每满减，2-折扣，3-无门槛，4-满减
     */
    private Integer discountType;

    /**
     * 满减门槛金额（单位：分）
     */
    private Integer thresholdAmount;

    /**
     * 最大优惠金额（单位：元）
     */
    private BigDecimal maxDiscountAmount;

    /**
     * 折扣值，如果是满减则存满减金额，如果是折扣，则存折扣率，8折就是存80
     */
    private Integer discountValue;
}