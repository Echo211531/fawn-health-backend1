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
 * 退款申请表
 * @TableName refund_application
 */
@TableName(value ="refund_application")
@Data
public class RefundApplication implements Serializable {
    /**
     * 退款ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 退款类型：1-仅退款，2-退货退款
     */
    private Integer refundType;

    /**
     * 退款原因
     */
    private String refundReason;

    /**
     * 退款说明
     */
    private String refundRemark;

    /**
     * 退款状态：0-待处理，1-处理中，2-退款成功，3-退款失败，4-已取消
     */
    private Integer status;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 处理备注
     */
    private String handleNote;

    /**
     * 处理人ID
     */
    private Long handlerId;

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