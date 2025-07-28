package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

@Data
public class FoodPageQueryDTO {
    /**
     * 页码，默认第1页
     */
    private Long pageNum = 1L;

    /**
     * 每页条数，默认10条
     */
    private Long pageSize = 10L;

    /**
     * 食物ID（精确查询）
     */
    private Long foodId;

    /**
     * 食物名称（模糊查询）
     */
    private String foodName;

    /**
     * 分类名称（模糊查询，关联食物分类表）
     */
    private String categoryName;

    /**
     * 是否常见食物:0否,1是
     */
    private Integer isCommon;
}