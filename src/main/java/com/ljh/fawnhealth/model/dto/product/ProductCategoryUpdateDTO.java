package com.ljh.fawnhealth.model.dto.product;

import lombok.Data;

/**
 * 商品分类修改参数DTO
 */
@Data
public class ProductCategoryUpdateDTO {

    /**
     * 分类ID（必须传递，用于定位修改的分类）
     */
    private Long id;

    /**
     * 分类名称（支持修改）
     */
    private String name;

    /**
     * 分类图标URL（支持修改，可为空）
     */
    private String icon;

    /**
     * 分类描述（支持修改，可为空）
     */
    private String description;

    /**
     * 排序权重（支持修改，0-1000之间）
     */
    private Integer sortOrder;

    /**
     * 状态（支持修改：0-禁用，1-启用）
     */
    private Integer status;
}