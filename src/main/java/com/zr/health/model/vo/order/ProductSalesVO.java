package com.zr.health.model.vo.order;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品销量统计VO
 * 用于展示单个商品的销量信息
 */
@Data
public class ProductSalesVO {
    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片URL
     */
    private String productImage;

    /**
     * 销售总数量
     */
    private Integer totalSales;

    /**
     * 商品单价（下单时的价格）
     */
    private BigDecimal currentPrice;

    /**
     * 销售总金额（数量×单价）
     */
    private BigDecimal totalSalesAmount;
}