package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 会员订单表
 * @TableName vip_orders
 */
@TableName(value ="vip_orders")
@Data
public class VipOrders implements Serializable {
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
     * 会员类型:1月卡,2季卡,3年卡
     */
    private Integer vipType;

    /**
     * 会员方案ID（预留字段）
     */
    private Long vipPlanId;

    /**
     * 订单金额
     */
    private BigDecimal amount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 最终支付金额
     */
    private BigDecimal finalAmount;

    /**
     * 支付方式:1微信,2支付宝,3苹果支付
     */
    private Integer paymentMethod;

    /**
     * 第三方支付订单号
     */
    private String tradeNo;

    /**
     * 支付时间
     */
    private Date paymentTime;

    /**
     * 状态:0未支付,1已支付,2已取消,3已退款
     */
    private Integer status;

    /**
     * 会员开始时间
     */
    private Date startTime;

    /**
     * 会员结束时间
     */
    private Date endTime;

    /**
     * 使用的优惠券ID
     */
    private Long couponId;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款时间
     */
    private Date refundTime;

    /**
     * 订单来源:1App,2小程序,3H5,4后台
     */
    private Integer source;

    /**
     * 是否为自动续费订单: 0否, 1是
     */
    private Integer isRenewal;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}