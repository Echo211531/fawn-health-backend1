package com.ljh.fawnhealth.model.dto.order;

import lombok.Data;

/**
 * 订单确认收货请求参数
 */
@Data
public class OrderConfirmReceiveDTO {

    /**
     * 订单ID（必填）
     */
    private Long orderId;

    /**
     * 收货备注（可选）
     */
    private String receiveNote;
}