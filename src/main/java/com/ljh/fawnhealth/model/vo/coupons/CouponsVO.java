package com.ljh.fawnhealth.model.vo.coupons;


import com.ljh.fawnhealth.model.enums.coupons.DiscountType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端优惠券信息
 */
@Data
public class CouponsVO {

    /**
     * 优惠券id，新增不需要添加，更新必填
     */
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 是否限定使用范围
     */
    private Boolean specific;

    /**
     * 优惠券类型，1：每满减，2：折扣，3：无门槛，4：普通满减
     */
    private DiscountType discountType;

    /**
     * 折扣门槛，0代表无门槛
     */
    private Integer thresholdAmount;

    /**
     * 折扣值，满减填抵扣金额；打折填折扣值：80标示打8折
     */
    private Integer discountValue;

    /**
     * 最大优惠金额
     */
    private Integer maxDiscountAmount;

    /**
     * 有效天数
     */
    private Integer termDays;

    /**
     * 使用有效期结束时间
     */
    private LocalDateTime termEndTime;

    /**
     * 是否可以领取
     */
    private Boolean available;

    /**
     * 是否可以使用
     */
    private Boolean received;
}
