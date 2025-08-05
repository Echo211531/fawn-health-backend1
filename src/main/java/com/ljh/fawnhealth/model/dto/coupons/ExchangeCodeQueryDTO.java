package com.ljh.fawnhealth.model.dto.coupons;

import lombok.Data;

@Data
public class ExchangeCodeQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /**
     *  兑换码状态（0-未使用，1-已使用，2-已过期）
     */
    private Integer status;

    private Long couponId;
}