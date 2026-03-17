package com.zr.health.model.dto.order;

import lombok.Data;

import java.util.List;

/**
 * 创建订单的请求参数DTO
 */
@Data
public class OrderCreateDTO {

    private Long userId;

    /**
     * 收货地址ID（关联shipping_address表）
     */
    private Long addressId;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 支付方式：1-支付宝，2-微信，3-银联
     */
    private Integer paymentType;

    /**
     * 订单来源：1-PC，2-APP，3-小程序，4-H5
     */
    private Integer source;

    /**
     * 使用的优惠券ID（可选）
     */
    private Long couponId;

    /**
     * 订单项列表（商品信息）
     */
    private List<OrderItemDTO> orderItems;
}