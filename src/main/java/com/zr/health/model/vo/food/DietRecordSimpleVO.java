package com.zr.health.model.vo.food;

import com.zr.health.model.dto.food.DietFoodItemDTO;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 饮食记录简要视图对象
 * 用于列表页展示饮食记录的基础信息，包含关联的食物列表简要信息
 */
@Data
public class DietRecordSimpleVO {
    /**
     * 饮食记录ID
     */
    private Long id;

    /**
     * 餐次类型
     * 1 - 早餐
     * 2 - 午餐
     * 3 - 晚餐
     * 4 - 加餐
     */
    private Integer mealType;

    /**
     * 记录日期，仅日期部分
     */
    private Date recordDate;

    /**
     * 记录时间，包含具体时间信息
     */
    private Date recordTime;

    /**
     * 备注信息，可为空
     */
    private String note;

    /**
     * 关联的食物列表，包含该饮食记录下所有食物的简要信息
     */
    private List<DietFoodItemDTO> foodItems;
}
