package com.zr.health.model.vo.order;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项响应视图对象
 */
@Data
public class OrderItemVO {

    /**
     * 订单项ID
     */
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 下单时的商品单价
     */
    private BigDecimal currentPrice;

    /**
     * 购买数量
     */
    private Integer quantity;

    /**
     * 商品总价（单价×数量）
     */
    private BigDecimal totalPrice;

    /**
     * 商品规格
     */
    private String specs;
}