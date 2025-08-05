package com.ljh.fawnhealth.model.dto.order;

import lombok.Data;

/**
 * 管理员审核退款参数
 */
@Data
public class OrderRefundAuditDTO {
    /**
     * 订单ID（必填）
     */
    private Long orderId;

    /**
     * 审核结果：1-通过，2-驳回（必填）
     */
    private Integer auditResult;

    /**
     * 审核备注（驳回时必填，说明原因）
     */
    private String auditRemark;
}