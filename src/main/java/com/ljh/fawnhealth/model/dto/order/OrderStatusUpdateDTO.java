package com.ljh.fawnhealth.model.dto.order;

import lombok.Data;

/**
 * 订单状态修改DTO
 */
@Data
public class OrderStatusUpdateDTO {
    /**
     * 订单ID（必填）
     */
    private Long orderId;

    /**
     * 目标状态（必填）
     * 0-待支付，1-已支付待发货，2-已发货，3-已完成，4-已取消，5-已退款，6-已关闭
     */
    private Integer targetStatus;

    /**
     * 物流公司（仅状态为2-已发货时必填）
     */
    private String deliveryCompany;

    /**
     * 物流单号（仅状态为2-已发货时必填）
     */
    private String deliveryNo;

    /**
     * 操作备注（可选）
     */
    private String remark;
}