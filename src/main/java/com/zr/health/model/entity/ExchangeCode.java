package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 兑换码表
 * @TableName exchange_code
 */
@TableName(value ="exchange_code")
@Data
public class ExchangeCode implements Serializable {
    /**
     * 兑换码id
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 兑换码状态，1：待兑换，2：已兑换，3：兑换活动已结束
     */
    private Integer status;

    /**
     * 兑换人
     */
    private Long userId;

    /**
     * 兑换类型，1：优惠券，以后再添加其它类型
     */
    private Integer type;

    /**
     * 兑换码目标id，例如兑换优惠券，该id则是优惠券的配置id
     */
    private Long exchangeTargetId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 兑换码过期时间
     */
    private Date expiredTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}