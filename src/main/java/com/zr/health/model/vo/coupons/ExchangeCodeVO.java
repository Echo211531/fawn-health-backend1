package com.zr.health.model.vo.coupons;

import lombok.Data;

@Data
public class ExchangeCodeVO {
    private Long id;

    private String code;

    /**
     * 状态（0-未使用，1-已使用，2-已过期
     */
    private Integer status;

    /**
     * 过期时间
     */
    private String expiredTime;
}