package com.ljh.fawnhealth.model.vo.product;

import com.ljh.fawnhealth.model.entity.Cart;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 购物车视图对象（包含商品详情）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CartVO extends Cart {
    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品单价
     */
    private BigDecimal productPrice;

    /**
     * 商品图片
     */
    private String productImage;

    /**
     * 商品状态（1-上架，0-下架，2-缺货）
     */
    private Integer productStatus;
}
