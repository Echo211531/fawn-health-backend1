package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.product.ProductCategoryCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductCategoryUpdateDTO;
import com.ljh.fawnhealth.model.entity.ProductCategory;
import com.ljh.fawnhealth.model.vo.product.ProductCategoryVO;
import com.ljh.fawnhealth.model.vo.product.ProductVO;

import java.util.List;


/**
* @author 27105
* @description 针对表【product_category(商品分类表)】的数据库操作Service
* @createDate 2025-07-14 23:00:12
*/
public interface ProductCategoryService extends IService<ProductCategory> {

    /**
     * 创建商品分类
     * 分类层级和父分类ID无需前端传递，默认创建一级分类
     *
     * @param createDTO 分类创建参数
     * @return 操作结果
     */
    void createProductCategory(ProductCategoryCreateDTO createDTO);

    /**
     * 启用/禁用商品分类
     *
     * @param categoryId 分类ID
     * @param status     状态：0-禁用，1-启用
     * @return 操作结果
     */
    void updateCategoryStatus(Long categoryId, Integer status);

    /**
     * 删除商品分类（逻辑删除）
     *
     * @param categoryId 分类ID
     * @return 操作结果
     */
    void deleteProductCategory(Long categoryId);

    /**
     * 修改商品分类信息
     * 支持修改：名称、图标、描述、排序权重、状态
     *
     * @param updateDTO 修改参数（包含分类ID和待修改的字段）
     * @return 操作结果
     */
    void updateProductCategory(ProductCategoryUpdateDTO updateDTO);

    /**
     * 查询商品分类列表
     * 支持按名称（模糊查询）和状态（精确查询）筛选，参数全不传则返回所有分类
     *
     * @param name   分类名称（可选，模糊查询）
     * @param status 分类状态（可选，0-禁用，1-启用）
     * @return 分类列表
     */
    List<ProductCategoryVO> getCategoryList(String name, Integer status);

    /**
     * 根据分类ID查询旗下商品列表
     *
     * @param categoryId 分类ID（必填）
     * @return 该分类下的商品列表
     */
    List<ProductVO> getProductsByCategoryId(Long categoryId);
}
