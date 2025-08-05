package com.ljh.fawnhealth.model.dto.product;

import lombok.Data;

@Data
public class CartAddDTO {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 数量（大于0）
     */
    private Integer quantity;
}
