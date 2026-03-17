package com.zr.health.model.dto.product;

import lombok.Data;

@Data
public class ProductUpdateStatusDTO {
    /**
     * 商品ID
     */
    private Long productId;
    /**
     * 要更新的状态（1 上架，0 下架 等，需与数据库定义匹配）
     */
    private Integer status;
}