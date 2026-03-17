package com.zr.health.model.dto.product;

import lombok.Data;

@Data
public class ProductPageQueryDTO {
    /**
     * 商品ID（精确匹配，可选）
     */
    private Long id;

    /**
     * 商品名称（模糊匹配，可选）
     */
    private String name;

    /**
     * 商品状态（0-下架，1-上架，2-缺货，可选）
     */
    private Integer status;

    /**
     * 页码（默认1）
     */
    private Integer pageNum = 1;

    /**
     * 每页条数（默认10，最大100）
     */
    private Integer pageSize = 10;
}