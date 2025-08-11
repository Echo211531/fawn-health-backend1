package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 饮食记录中的单个食物项数据传输对象
 * 用于传递饮食记录中每个食物的详细信息
 */
@Data
public class DietFoodItemDTO {

    /**
     * 食物ID（用户选择的食物）
     */
    private Long foodId;

    /**
     * 食用量（单位：克或份）
     */
    private BigDecimal amount;

    /**
     * 单位（前端可选 “g” 或 “份”，后端可结合 foodLibrary 标准量处理）
     */
    private String unit;

    /**
     * 用户备注（可选）
     */
    private String note;

    /**
     * 食物名称
     */
    private String foodName;

    /**
     * 食物图片
     */
    private String foodImage;

    /**
     * 分类名称（用于识别蔬菜等类别）
     */
    private String categoryName;

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
}
