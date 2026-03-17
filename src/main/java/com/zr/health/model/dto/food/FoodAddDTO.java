package com.zr.health.model.dto.food;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 食物添加数据传输对象（DTO），用于封装在添加新食物到食物库时所需的信息。。
 */
@Data
public class FoodAddDTO {
    /**
     * 食物名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

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
