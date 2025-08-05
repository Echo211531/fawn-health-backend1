package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 食物库表对应的实体类，用于封装食物的相关信息。
 * @TableName food_library
 */
@TableName(value ="food_library")
@Data
public class FoodLibrary implements Serializable {
    /**
     * 食物ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 食物名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 热量(kcal/100g)
     */
    private BigDecimal calories;

    /**
     * 蛋白质(g/100g)
     */
    private BigDecimal protein;

    /**
     * 脂肪(g/100g)
     */
    private BigDecimal fat;

    /**
     * 碳水化合物(g/100g)
     */
    private BigDecimal carbohydrate;

    /**
     * 膳食纤维(g/100g)
     */
    private BigDecimal fiber;

    /**
     * 图片URL
     */
    private String image;

    /**
     * 是否常见食物:0否,1是
     */
    private Integer isCommon;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除:0否,1是
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}