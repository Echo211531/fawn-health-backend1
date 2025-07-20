package com.ljh.fawnhealth.model.vo.order;

import com.ljh.fawnhealth.model.vo.address.AddressVO;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单响应视图对象
 */
@Data
public class OrderVO {

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额（总金额-优惠-运费等）
     */
    private BigDecimal paymentAmount;

    /**
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 支付方式：1-支付宝，2-微信，3-银联
     */
    private Integer paymentType;

    /**
     * 订单状态：0-待支付，1-已支付待发货，2-已发货，3-已完成，4-已取消，5-已退款，6-已关闭
     */
    private Integer status;

    /**
     * 订单状态描述（如：待支付）
     */
    private String statusDesc;

    /**
     * 收货地址信息
     */
    private AddressVO addressVO;

    /**
     * 订单项列表
     */
    private List<OrderItemVO> orderItems;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 创建时间
     */
    private Date createTime;
}