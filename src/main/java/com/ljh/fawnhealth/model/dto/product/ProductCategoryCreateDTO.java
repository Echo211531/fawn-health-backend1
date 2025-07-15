package com.ljh.fawnhealth.model.dto.product;

import lombok.Data;

/**
 * 商品分类创建参数DTO
 */
@Data
public class ProductCategoryCreateDTO {

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类图标URL
     */
    private String icon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重
     * 默认为0，数值越大排序越靠前
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     * 默认为1（启用）
     */
    private Integer status = 1;
}
