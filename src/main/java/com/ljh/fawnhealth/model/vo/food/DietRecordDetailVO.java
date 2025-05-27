package com.ljh.fawnhealth.model.vo.food;

import com.ljh.fawnhealth.model.dto.food.DietFoodItemDTO;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 饮食记录详细视图对象
 * 用于展示某条饮食记录的完整信息，包括基本信息和食物项列表
 */
@Data
public class DietRecordDetailVO {
    /**
     * 饮食记录ID
     */
    private Long id;

    /**
     * 用户ID，表示该记录所属用户
     */
    private Long userId;

    /**
     * 餐次类型
     * 1 - 早餐
     * 2 - 午餐
     * 3 - 晚餐
     * 4 - 加餐
     */
    private Integer mealType;

    /**
     * 记录日期，仅包含日期部分
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
     * 食物项列表，包含该饮食记录下所有的食物详细信息
     */
    private List<DietFoodItemDTO> foodItems;
}
