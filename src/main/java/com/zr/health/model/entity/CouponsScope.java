package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 优惠券使用范围表
 * @TableName coupons_scope
 */
@TableName(value ="coupons_scope")
@Data
public class CouponsScope implements Serializable {
    /**
     * ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 范围限定类型：1-分类，等等
     */
    private Integer type;

    /**
     * 优惠券ID
     */
    private Long couponId;

    /**
     * 优惠券作用范围的业务id,例如商品的分类id
     */
    private Long bizId;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}