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
 * 饮食记录主表
 * @TableName diet_records
 */
@TableName(value ="diet_records")
@Data
public class DietRecords implements Serializable {
    /**
     * 主记录ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 餐次类型（1早餐，2午餐，3晚餐，4加餐）
     */
    private Integer mealType;

    /**
     * 记录日期（仅日期部分）
     */
    private Date recordDate;

    /**
     * 记录时间（具体时刻）
     */
    private Date recordTime;

    /**
     * 备注信息
     */
    private String note;

    /**
     * 热量
     */
    private BigDecimal totalCalories;

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