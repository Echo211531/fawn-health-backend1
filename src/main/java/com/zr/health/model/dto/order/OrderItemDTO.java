package com.zr.health.model.dto.order;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单项请求参数DTO
 */
@Data
public class OrderItemDTO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 购买数量（至少1件）
     */
    private Integer quantity;

    /**
     * 下单时的商品单价（前端传递，后端需校验）
     */
    private BigDecimal currentPrice;

    /**
     * 商品规格（JSON格式，如：{"颜色":"红色","尺寸":"XL"}）
     */
    private String specs;
}