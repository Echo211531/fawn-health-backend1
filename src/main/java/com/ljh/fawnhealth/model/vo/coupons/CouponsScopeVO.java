package com.ljh.fawnhealth.model.vo.coupons;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 优惠券使用范围
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponsScopeVO {
    /**
     * 范围id
     */
    private Long id;
    /**
     * 范围名称
     */
    private String name;
}