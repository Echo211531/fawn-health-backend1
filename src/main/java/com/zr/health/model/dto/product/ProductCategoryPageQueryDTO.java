package com.zr.health.model.dto.product;

import lombok.Data;

@Data
public class ProductCategoryPageQueryDTO {
    /**
     * 页码，默认第 1 页
     */
    private Long pageNum = 1L;
    /**
     * 每页条数，默认 10 条
     */
    private Long pageSize = 10L;
    /**
     * 分类 ID（精确查询）
     */
    private Long categoryId;
    /**
     * 分类名称（模糊查询）
     */
    private String name;
    /**
     * 分类状态（精确查询，0-禁用，1-启用）
     */
    private Integer status;
}