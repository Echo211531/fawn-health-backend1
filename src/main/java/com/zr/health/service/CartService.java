package com.zr.health.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.entity.Cart;
import com.zr.health.model.vo.product.CartVO;

import java.util.List;


/**
* @author 27105
* @description 针对表【cart(购物车表)】的数据库操作Service
* @createDate 2025-07-14 23:01:15
*/
public interface CartService extends IService<Cart> {

    /**
     * 添加商品到购物车
     * @param userId 用户ID
     * @param productId 商品ID
     * @param quantity 数量
     */
    void addToCart(Long userId, Long productId, Integer quantity);

    /**
     * 从购物车移除商品
     *
     * @param userId 用户ID
     * @param productId 商品ID
     */
    void removeFromCart(Long userId, Long productId);

    /**
     * 将购物车中某商品数量更新为指定值（用于前端步进器）
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  新数量，必须大于等于 1
     */
    void updateCartItemQuantity(Long userId, Long productId, Integer quantity);

    /**
     * 查询用户购物车列表（包含商品详情）
     *
     * @param userId 用户ID
     * @return 购物车列表（包含商品信息）
     */
    List<CartVO> getUserCartList(Long userId);
}
