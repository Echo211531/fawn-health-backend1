package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单表
 * @TableName order
 */
@TableName(value ="order")
@Data
public class Order implements Serializable {
    /**
     * 订单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 实付金额
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
     * 优惠券抵扣金额
     */
    private BigDecimal couponAmount;

    /**
     * 支付方式：1-支付宝，2-微信，3-银联
     */
    private Integer paymentType;

    /**
     * 支付时间
     */
    private Date paymentTime;

    /**
     * 支付流水号
     */
    private String paymentSerialNumber;

    /**
     * 订单状态：0-待支付，1-已支付待发货，2-已发货，3-已完成，4-已取消，5-已退款，6-已关闭
     */
    private Integer status;

    /**
     * 物流公司
     */
    private String deliveryCompany;

    /**
     * 物流单号
     */
    private String deliveryNo;

    /**
     * 发货时间
     */
    private Date deliveryTime;

    /**
     * 收货时间
     */
    private Date receiveTime;

    /**
     * 订单备注
     */
    private String note;

    /**
     * 订单来源：1-PC，2-APP，3-小程序，4-H5
     */
    private Integer source;

    /**
     * 确认收货状态：0-未确认，1-已确认
     */
    private Integer confirmStatus;

    /**
     * 删除状态：0-未删除，1-已删除
     */
    private Integer deleteStatus;

    /**
     * 使用的优惠券ID
     */
    private Long couponId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除：0-否，1-是
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}