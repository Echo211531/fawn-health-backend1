package com.ljh.fawnhealth.model.query.coupons;


import com.ljh.fawnhealth.commen.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

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