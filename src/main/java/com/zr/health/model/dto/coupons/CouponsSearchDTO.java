package com.zr.health.model.dto.coupons;

import lombok.Data;

@Data
public class CouponsSearchDTO {
    /**
     * 页码，默认1
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10
     */
    private Integer pageSize = 10;

    /**
     * 优惠券折扣类型：1：每满减，2：折扣，3：无门槛，4：满减
     */
    private Integer discountType;

    /**
     * 优惠券状态，1：待发放，2：发放中，3：已结束, 4：暂停
     */
    private Integer status;

    /**
     * 优惠券名称（模糊查询）
     */
    private String name;
}
