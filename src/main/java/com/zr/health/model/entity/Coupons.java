package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 优惠券表
 * @TableName coupons
 */
@TableName(value ="coupons")
@Data
public class Coupons implements Serializable {
    /**
     * 优惠券ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券类型，1：普通券。目前就一种，保留字段
     */
    private Integer type;

    /**
     * 折扣类型，1：满减，2：每满减，3：折扣，4：无门槛
     */
    private Integer discountType;

    /**
     * 是否限定作用范围，false：不限定，true：限定。默认false
     */
    @TableField("`specific`")
    private Boolean specific;

    /**
     * 折扣值，如果是满减则存满减金额，如果是折扣，则存折扣率，8折就是存80
     */
    private Integer discountValue;

    /**
     * 使用门槛，0：表示无门槛，其他值：最低消费金额
     */
    private Integer thresholdAmount;

    /**
     * 最高优惠金额，满减最大，0：表示没有限制，不为0，则表示该券有金额上限
     */
    private BigDecimal maxDiscountAmount;

    /**
     * 获取方式：1：手动领取，2：兑换码
     */
    private Integer obtainWay;

    /**
     * 开始发放时间
     */
    private Date issueBeginTime;

    /**
     * 结束发放时间
     */
    private Date issueEndTime;

    /**
     * 优惠券有效期天数，0：表示有效期是指定有效期的
     */
    private Integer termDays;

    /**
     * 优惠券有效期开始时间
     */
    private Date termBeginTime;

    /**
     * 优惠券有效期结束时间
     */
    private Date termEndTime;

    /**
     * 优惠券配置状态，1：待发放，2：未开始，3：进行中，4：已结束，5：未知（可根据实际情况调整）
     */
    private Integer status;

    /**
     * 总数量，不超过5000
     */
    private Integer totalNum;

    /**
     * 已发行数量，用于判断是否超发
     */
    private Integer issueNum;

    /**
     * 已使用数量
     */
    private Integer usedNum;

    /**
     * 每个人限领的数量，默认1
     */
    private Integer userLimit;

    /**
     * 拓展参数字段，保留字段
     */
    private String extParam;

    /**
     * 优惠券描述
     */
    private String description;

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

    /**
     * 创建人
     */
    private Long creater;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}