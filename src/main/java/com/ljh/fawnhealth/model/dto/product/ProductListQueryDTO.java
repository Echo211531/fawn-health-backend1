package com.ljh.fawnhealth.model.dto.product;

import lombok.Data;

/**
 * 商品列表查询参数DTO
 */
@Data
public class ProductListQueryDTO {

    /**
     * 商品名称（模糊查询）
     */
    private String name;

    /**
     * 商品状态：0-下架，1-上架，2-缺货
     */
    private Integer status;

    /**
     * 是否热销：0-否，1-是
     */
    private Integer isHot;

    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommend;
}