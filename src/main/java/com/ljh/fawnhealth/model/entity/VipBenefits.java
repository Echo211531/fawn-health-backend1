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
 * VIP权益表
 * @TableName vip_benefits
 */
@TableName(value ="vip_benefits")
@Data
public class VipBenefits implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 会员类型: 1月卡, 2季卡, 3年卡（或绑定vip_orders的vip_type）
     */
    private Integer vipType;

    /**
     * 会员价格
     */
    private BigDecimal price;

    /**
     * 权益编码，例如：DAILY_REPORT、NO_ADS、UNLOCK_RECIPES
     */
    private String benefitCode;

    /**
     * 权益名称
     */
    private String benefitName;

    /**
     * 权益描述
     */
    private String description;

    /**
     * 权益值（如次数、额度、期限等）
     */
    private String value;

    /**
     * 展示顺序
     */
    private Integer sortOrder;

    /**
     * 是否启用: 0停用, 1启用
     */
    private Integer status;

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