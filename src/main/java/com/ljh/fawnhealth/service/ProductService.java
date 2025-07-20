package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.product.ProductCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductListQueryDTO;
import com.ljh.fawnhealth.model.dto.product.ProductUpdateDTO;
import com.ljh.fawnhealth.model.entity.Product;
import com.ljh.fawnhealth.model.vo.product.ProductVO;

import java.util.List;


/**
* @author 27105
* @description 针对表【product(商品表)】的数据库操作Service
* @createDate 2025-07-14 22:59:34
*/
public interface ProductService extends IService<Product> {

    /**
     * 管理员创建商品
     *
     * @param dto 商品创建参数
     * @return 操作结果
     */
    void createProduct(ProductCreateDTO dto);

    /**
     * 更新食物状态
     *
     * @param productId 商品ID
     * @param status    状态（1=上架，0=下架，2=缺货）
     * @return 操作结果
     */
    void updateFoodStatus(Long productId, Integer status);

    /**
     * 修改商品信息（DTO中包含商品ID）
     *
     * @param updateDTO 商品修改参数（包含商品ID）
     * @return 操作结果
     */
    void updateProduct(ProductUpdateDTO updateDTO);

    /**
     * 逻辑删除商品
     *
     * @param productId 商品ID
     */
    void deleteProduct(Long productId);

    /**
     * 根据多条件查询商品列表
     *
     * @param queryDTO 查询参数（包含状态、名称、是否热销、是否推荐）
     * @return 商品信息列表
     */
    /**
     * 根据零散参数查询商品列表
     * @param name 商品名称（模糊）
     * @param status 状态
     * @param isHot 是否热销
     * @param isRecommend 是否推荐
     * @return 商品VO列表
     */
    List<ProductVO> getProductListByParams(String name, Integer status, Integer isHot, Integer isRecommend);

    /**
     * 根据商品ID查询商品详细信息
     *
     * @param productId 商品ID
     * @return 商品详细信息（ProductVO）
     */
    ProductVO getProductById(Long productId);

    /**
     * 查询推荐商品列表
     * 只查询状态为上架（status=1）且标记为推荐（is_recommend=1）的商品
     *
     * @return 推荐商品列表
     */
    List<ProductVO> getRecommendProducts();

    /**
     * 根据商品名称模糊查询商品分类信息
     * 只查询状态为上架（status=1）且未删除（is_delete=0）的商品
     *
     * @param name 商品名称（模糊匹配）
     * @return 符合条件的商品分类信息列表
     */
    List<ProductVO> searchProductsByCategory(String name);
}
