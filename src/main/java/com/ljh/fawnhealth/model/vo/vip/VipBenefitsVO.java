package com.ljh.fawnhealth.model.vo.vip;

import lombok.Data;

import java.math.BigDecimal;

/**
 * VIP会员权益视图对象
 * 封装了不同类型VIP会员对应的权益信息，用于前端展示
 */
@Data
public class VipBenefitsVO {

    /**
     * 会员类型: 1月卡, 2季卡, 3年卡（或绑定vip_orders的vip_type）
     */
    private Integer vipType;

    /**
     * 会员价格
     */
    private BigDecimal price;

    /**
     * 权益名称
     */
    private String benefitName;

    /**
     * 权益描述
     */
    private String description;

    /**
     * 权益值（如次数、额度、期限等）
     */
    private String value;
}