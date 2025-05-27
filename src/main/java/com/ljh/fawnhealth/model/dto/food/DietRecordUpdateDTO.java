package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 饮食记录更新请求DTO
 * 用于封装用户更新饮食记录的请求参数，包含需要修改的饮食记录信息
 */
@Data
public class DietRecordUpdateDTO {
    /**
     * 饮食记录ID，必填
     * 用于唯一标识一条饮食记录，定位需要修改的具体记录
     */
    private Long id; // 饮食记录ID，必传，用于定位要修改哪条记录

    /**
     * 记录日期（仅日期部分）
     * 若为空则不更新该字段
     */
    private Date recordDate;

    /**
     * 餐次类型（1-早餐，2-午餐，3-晚餐，4-加餐）
     * 若为空则不更新该字段
     */
    private Integer mealType;

    /**
     * 备注信息
     * 若为空则不更新该字段
     */
    private String note;

    /**
     * 食物项列表，包含多个食物的详细信息
     * 若为空则不更新该字段
     */
    private List<DietFoodItemUpdateDTO> foodItems;
}