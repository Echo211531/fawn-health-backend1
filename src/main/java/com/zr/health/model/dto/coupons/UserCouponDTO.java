package com.zr.health.model.dto.coupons;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCouponDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;

    private String requestId;
}
