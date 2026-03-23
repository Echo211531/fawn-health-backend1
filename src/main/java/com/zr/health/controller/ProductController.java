package com.zr.health.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.model.dto.product.*;
import com.zr.health.model.vo.product.CartVO;
import com.zr.health.model.vo.product.ProductVO;
import com.zr.health.service.CartService;
import com.zr.health.service.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品模块
 * 提供商品的创建、上架\下架商品等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @Resource
    private CartService cartService;

    /**
     * 管理员创建商品
     *
     * @param dto 商品创建参数
     * @return 操作结果
     */
    @PostMapping("/createProduct")
    public BaseResponse<String> createProduct(@RequestBody ProductCreateDTO dto) {
        productService.createProduct(dto);
        return ResultUtils.success("商品创建成功");
    }

    /**
     * 更新食物状态
     *
     * @param productId 商品ID
     * @param status    状态（1=上架，0=下架，2=缺货）
     * @return 操作结果
     */
    @GetMapping("/updateFoodStatus/{productId}/{status}")
    public BaseResponse<String> updateFoodStatus(@PathVariable Long productId, @PathVariable Integer status) {
        productService.updateFoodStatus(productId, status);
        return ResultUtils.success("食物状态更新成功");
    }

    /**
     * 修改商品信息（DTO中包含商品ID）
     *
     * @param updateDTO 商品修改参数（包含商品ID）
     * @return 操作结果
     */
    @PostMapping("/update")
    public BaseResponse<String> updateProduct(@RequestBody ProductUpdateDTO updateDTO) {
        // 执行修改操作
        productService.updateProduct(updateDTO);
        log.info("商品信息修改成功，商品ID: {}", updateDTO.getId());
        return ResultUtils.success("商品信息修改成功");
    }

    /**
     * 删除商品（逻辑删除）
     *
     * @param productId 商品ID
     * @return 操作结果
     */
    @GetMapping("/delete/{productId}")
    public BaseResponse<String> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        log.info("商品删除成功，商品ID: {}", productId);
        return ResultUtils.success("商品删除成功");
    }

    /**
     * 根据多条件查询商品列表（直接传递字段参数，支持全部为空）
     *
     * @param name        商品名称（模糊查询，可选）
     * @param status      商品状态：0-下架，1-上架，2-缺货（可选）
     * @param isHot       是否热销：0-否，1-是（可选）
     * @param isRecommend 是否推荐：0-否，1-是（可选）
     * @return 商品信息列表
     */
    @GetMapping("/list")
    public BaseResponse<List<ProductVO>> getProductList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer isHot,
            @RequestParam(required = false) Integer isRecommend) {

        // 构建查询条件（参数全部可选，为空则不参与筛选）
        List<ProductVO> productList = productService.getProductListByParams(
                name, status, isHot, isRecommend);

        return ResultUtils.success(productList);
    }

    /**
     * 根据商品ID查询商品详细信息
     *
     * @param productId 商品ID
     * @return 商品详细信息（ProductVO）
     */
    @GetMapping("/detail/{productId}")
    public BaseResponse<ProductVO> getProductDetail(@PathVariable Long productId) {
        ProductVO productVO = productService.getProductById(productId);
        return ResultUtils.success(productVO);
    }

    /**
     * 查询推荐商品列表
     * 只查询状态为上架（status=1）且标记为推荐（is_recommend=1）的商品
     *
     * @return 推荐商品列表
     */
    @GetMapping("/recommend")
    public BaseResponse<List<ProductVO>> getRecommendProducts() {
        // 查询状态为上架且标记为推荐的商品
        List<ProductVO> recommendProducts = productService.getRecommendProducts();
        return ResultUtils.success(recommendProducts);
    }

    /**
     * 根据商品名称模糊查询商品分类信息
     * 只查询状态为上架（status=1）且未删除（is_delete=0）的商品
     *
     * @param name 商品名称（模糊匹配）
     * @return 符合条件的商品分类信息列表
     */
    @GetMapping("/category/search")
    public BaseResponse<List<ProductVO>> searchProductCategoriesByName(@RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品名称不能为空");
        }

        List<ProductVO> productVOs = productService.searchProductsByCategory(name.trim());
        return ResultUtils.success(productVOs);
    }

    /**
     * 分页查询商品列表（支持多条件筛选）
     * 筛选条件：商品ID（精确）、商品名称（模糊）、商品状态（精确）
     * 仅查询未删除（is_delete=0）的商品
     *
     * @param queryDTO 包含查询条件和分页参数
     * @return 分页结果（包含商品列表和分页信息）
     */
    @PostMapping("/page")
    public BaseResponse<IPage<ProductVO>> pageQueryProducts(@RequestBody ProductPageQueryDTO queryDTO) {
        log.info("分页查询商品列表，参数：{}", queryDTO);

        // 校验分页参数合法性
        if (queryDTO.getPageNum() < 1) {
            queryDTO.setPageNum(1); // 页码默认1
        }
        if (queryDTO.getPageSize() < 1 || queryDTO.getPageSize() > 100) {
            queryDTO.setPageSize(10); // 每页条数默认10，最大100
        }

        IPage<ProductVO> productPage = productService.pageQueryProducts(queryDTO);
        return ResultUtils.success(productPage);
    }

    /**
     * 通用更新商品状态接口（上架、下架等，通过参数区分）
     * @param updateStatusDTO 包含商品ID和要更新的状态
     * @return 操作结果
     */
    @PostMapping("/updateStatus")
    public BaseResponse<String> updateProductStatus(@RequestBody ProductUpdateStatusDTO updateStatusDTO) {
        Long productId = updateStatusDTO.getProductId();
        Integer status = updateStatusDTO.getStatus();
        // 简单参数校验，可根据实际需求完善
        if (productId == null || status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID和状态参数不合法");
        }
        productService.updateFoodStatus(productId, status);
        String message = status == 1 ? "商品上架成功" : "商品下架成功";
        log.info(message + "，商品ID: {}", productId);
        return ResultUtils.success(message);
    }

    /**
     * 添加商品到购物车
     *
     * @param cartDTO
     * @param userId
     * @return
     */
    @PostMapping("/cart/add")
    public BaseResponse<String> addToCart(@RequestBody CartAddDTO cartDTO, @RequestParam Long userId) {
        cartService.addToCart(userId, cartDTO.getProductId(), cartDTO.getQuantity());
        log.info("商品添加到购物车成功，用户ID: {}, 商品ID: {}, 数量: {}",
                userId, cartDTO.getProductId(), cartDTO.getQuantity());
        return ResultUtils.success("商品已成功添加到购物车");
    }

    /**
     * 从购物车移除商品
     *
     * @param userId
     * @param productId
     * @return
     */
    @PostMapping("/cart/remove")
    public BaseResponse<String> removeFromCart(@RequestParam Long userId, @RequestParam Long productId) {
        // 调用服务层移除购物车商品
        cartService.removeFromCart(userId, productId);
        log.info("商品从购物车移除成功，用户ID: {}, 商品ID: {}", userId, productId);
        return ResultUtils.success("商品已从购物车移除");
    }

    /**
     * 更新购物车中某商品的数量（步进器减/直接输入）
     *
     * @param userId    用户ID
     * @param productId 商品ID
     * @param quantity  新数量，须大于等于 1
     */
    @PostMapping("/cart/updateQuantity")
    public BaseResponse<String> updateCartQuantity(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        cartService.updateCartItemQuantity(userId, productId, quantity);
        log.info("购物车数量更新成功，用户ID: {}, 商品ID: {}, 数量: {}", userId, productId, quantity);
        return ResultUtils.success("数量已更新");
    }

    /**
     * 查询用户购物车列表（包含商品详情）
     *
     * @param userId 用户ID
     * @return 购物车列表（包含商品信息）
     */
    @GetMapping("/cart/list")
    public BaseResponse<List<CartVO>> getUserCartList(@RequestParam Long userId) {
        // 调用服务层查询用户购物车列表
        List<CartVO> cartList = cartService.getUserCartList(userId);
        log.info("用户查询购物车成功，用户ID: {}, 商品数量: {}", userId, cartList.size());
        return ResultUtils.success(cartList);
    }


}
