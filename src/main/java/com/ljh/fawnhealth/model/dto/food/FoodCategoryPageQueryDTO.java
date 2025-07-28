package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

/**
 * 食物分类分页查询DTO
 */
@Data
public class FoodCategoryPageQueryDTO {
    /**
     * 页码，默认第1页
     */
    private Long pageNum = 1L;

    /**
     * 每页条数，默认10条
     */
    private Long pageSize = 10L;

    /**
     * 分类ID（精确查询）
     */
    private Long categoryId;

    /**
     * 分类名称（模糊查询）
     */
    private String name;

    /**
     * 分类状态（精确查询：0-禁用，1-启用）
     */
    private Integer status;
}
