package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.CartMapper;
import com.ljh.fawnhealth.mapper.ProductMapper;
import com.ljh.fawnhealth.model.entity.Cart;
import com.ljh.fawnhealth.model.entity.Product;
import com.ljh.fawnhealth.model.vo.product.CartVO;
import com.ljh.fawnhealth.service.CartService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
* @author 27105
* @description 针对表【cart(购物车表)】的数据库操作Service实现
* @createDate 2025-07-14 23:01:15
*/
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart>
    implements CartService {

    @Resource
    private CartMapper cartMapper;

    @Resource
    private ProductMapper productMapper;

    /**
     * 添加商品到购物车
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  数量
     */
    @Override
    public void addToCart(Long userId, Long productId, Integer quantity) {
        // 1. 参数校验逻辑
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不合法");
        }
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品数量必须大于0");
        }

        // 2. 业务校验（商品是否存在、是否上架等）
        Product product = productMapper.selectById(productId);
        if (product == null || product.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在或已删除");
        }
        if (product.getStatus()!= 1) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "商品未上架，无法添加到购物车");
        }

        // 3. 核心业务逻辑（新增或更新购物车）
        Cart existingCart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (existingCart!= null) {
            // 商品已在购物车，更新数量
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            cartMapper.updateById(existingCart);
        } else {
            // 商品不在购物车，新增记录
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setSelected(1); // 默认选中
            cart.setSpecs(null); // 无需规格
            cartMapper.insert(cart);
        }
    }

    /**
     * 从购物车移除商品
     *
     * @param userId    用户ID
     * @param productId 商品ID
     */
    @Override
    public void removeFromCart(Long userId, Long productId) {
        // 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不合法");
        }

        // 检查购物车中是否存在该商品
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "购物车中不存在该商品");
        }

        // 执行删除（逻辑删除）
        cart.setIsDelete(1);
        int rows = cartMapper.updateById(cart);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "移除商品失败");
        }
    }

    /**
     * 查询用户购物车列表（包含商品详情）
     *
     * @param userId 用户ID
     * @return 购物车列表（包含商品信息）
     */
    @Override
    public List<CartVO> getUserCartList(Long userId) {
        // 1. 参数校验
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 查询用户购物车记录（未删除的）
        List<Cart> cartList = cartMapper.selectByUserId(userId);
        if (cartList.isEmpty()) {
            return Collections.emptyList(); // 返回空列表而非null
        }

        // 3. 关联查询商品详情并封装VO
        List<CartVO> result = new ArrayList<>(cartList.size());
        for (Cart cart : cartList) {
            // 查询商品详情
            Product product = productMapper.selectById(cart.getProductId());
            if (product == null) {
                log.warn("购物车商品不存在，商品ID: {}");
                continue;
            }

            // 封装购物车VO对象
            CartVO cartVO = new CartVO();
            // 复制购物车基本信息
            BeanUtils.copyProperties(cart, cartVO);
            // 设置商品详情
            cartVO.setProductName(product.getName());
            cartVO.setProductPrice(product.getPrice());
            cartVO.setProductImage(product.getMainImage());
            cartVO.setProductStatus(product.getStatus());

            result.add(cartVO);
        }

        return result;
    }
}




