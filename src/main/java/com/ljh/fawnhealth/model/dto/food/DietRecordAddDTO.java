package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 饮食记录新增请求DTO
 * 用于封装用户新增饮食记录的请求参数，包含餐次信息及具体食物项列表
 */
@Data
public class DietRecordAddDTO {
    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 餐次类型（1-早餐，2-午餐，3-晚餐，4-加餐）
     */
    private Integer mealType;

    /**
     * 备注信息
     */
    private String note;

    /**
     * 食物项列表，包含多个食物的详细信息
     */
    private List<DietFoodItemDTO> foodItems;
}