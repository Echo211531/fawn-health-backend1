package com.ljh.fawnhealth.model.dto.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单退款请求参数
 */
@Data
public class OrderRefundDTO {

    /**
     * 订单ID（必填）
     */
    private Long orderId;

    /**
     * 退款原因（必填，长度限制）
     */
    private String refundReason;

    /**
     * 退款金额（可选，默认全额退款）
     */
    private BigDecimal refundAmount;

    /**
     * 退款说明（可选，补充退款细节）
     */
    private String refundRemark;
}