package com.zr.health.model.dto.order;

import lombok.Data;

/**
 * 订单分页查询参数
 */
@Data
public class OrderPageQueryDTO {
    /**
     * 页码，默认第1页
     */
    private Integer pageNum = 1;

    /**
     * 每页条数，默认10条
     */
    private Integer pageSize = 10;

    /**
     * 订单状态（可选，null时查询所有状态）
     */
    private Integer status;

    /**
     * 订单ID（可选，精确匹配）
     */
    private String orderId;

    /**
     * 用户ID（可选，精确匹配）
     */
    private String userId;

    /**
     * 支付方式：1-支付宝，2-微信，3-银联
     */
    private Integer paymentType;

    /**
     * 订单来源（可选，null时查询所有来源）
     */
    private Integer source;
}