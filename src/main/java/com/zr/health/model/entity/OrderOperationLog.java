package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单操作日志表
 * @TableName order_operation_log
 */
@TableName(value ="order_operation_log")
@Data
public class OrderOperationLog implements Serializable {
    /**
     * 日志ID
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
     * 操作人，用户ID或管理员ID
     */
    private String operator;

    /**
     * 操作类型：1-创建订单，2-支付订单，3-发货，4-确认收货，5-取消订单，6-申请退款，7-退款成功，8-订单完成
     */
    private Integer operationType;

    /**
     * 操作备注
     */
    private String operationNote;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 是否删除：0-否，1-是
     */
    private Integer isDelete;


    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}