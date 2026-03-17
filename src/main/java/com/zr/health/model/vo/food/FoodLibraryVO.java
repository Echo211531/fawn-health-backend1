package com.zr.health.model.vo.food;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 食物库视图对象（VO），用于在展示层（如前端页面）展示食物相关信息。
 */
@Data
public class FoodLibraryVO {

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

    /**
     * 创建时间
     */
    private Date createTime;
}
