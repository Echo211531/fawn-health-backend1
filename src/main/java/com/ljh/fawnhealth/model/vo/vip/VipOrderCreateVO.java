package com.ljh.fawnhealth.model.vo.vip;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * VIP订单创建响应视图对象
 */
@Data
@Builder
public class VipOrderCreateVO {

    /**
     * 订单ID，数据库自增主键
     * */
    private Long orderId;

    /**
     * 订单编号，全局唯一标识（由系统生成，格式如：VIPORD20250527175832123）
     * */
    private String orderNo;

    /**
     * 操作结果状态
     * */
    private boolean success;

    /**
     * 实际支付金额（单位：元）
     */
    private BigDecimal paymentAmount;

    /**
     * 折扣金额（单位：元）
     */
    private BigDecimal discountAmount;

    /**
     * 支付链接
     */
    private String paymentUrl;

    /**
     * 二维码Base64字符串
     */
    private String qrCode;
}