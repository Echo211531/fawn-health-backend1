package com.zr.health.model.dto.food;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 食物更新数据传输对象（DTO），用于封装更新食物信息时所需的参数。
 */
@Data
public class FoodUpdateDTO {

    /**
     * 食物ID
     */
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
}
