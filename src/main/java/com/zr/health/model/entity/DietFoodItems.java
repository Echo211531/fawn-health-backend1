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
 * 饮食记录-食物项表
 * @TableName diet_food_items
 */
@TableName(value ="diet_food_items")
@Data
public class DietFoodItems implements Serializable {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联diet_records主键ID
     */
    private Long recordId;

    /**
     * 食物ID（可为空，若手动添加）
     */
    private Long foodId;

    /**
     * 食物名称
     */
    private String foodName;

    /**
     * 食用数量
     */
    private BigDecimal amount;

    /**
     * 单位（g/ml/份）
     */
    private String unit;

    /**
     * 热量(kcal)
     */
    private BigDecimal calories;

    /**
     * 蛋白质(g)
     */
    private BigDecimal protein;

    /**
     * 脂肪(g)
     */
    private BigDecimal fat;

    /**
     * 碳水化合物(g)
     */
    private BigDecimal carbohydrate;

    /**
     * 食物图片（多个用逗号分隔）
     */
    private String images;

    /**
     * 备注信息
     */
    private String note;

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