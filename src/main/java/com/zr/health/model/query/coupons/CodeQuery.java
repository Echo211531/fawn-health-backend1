package com.zr.health.model.query.coupons;

import com.zr.health.commen.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 兑换码查询条件封装类
 * 用于筛选和分页查询优惠券兑换码列表，继承分页基础查询类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CodeQuery extends PageQuery {

    /**
     * 兑换码对应的优惠券id
     */
    private Long couponId;

    /**
     * 兑换码状态，1：未兑换，2：已兑换
     */
    private Integer status;
}