package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.product.*;
import com.ljh.fawnhealth.model.vo.product.ProductCategoryVO;
import com.ljh.fawnhealth.model.vo.product.ProductVO;
import com.ljh.fawnhealth.service.ProductCategoryService;
import com.ljh.fawnhealth.service.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品分类模块
 * 提供商品分类的创建等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/productCategory")
public class ProductCategoryController {

    @Resource
    private ProductCategoryService productCategoryService;

    /**
     * 创建商品分类
     * 分类层级和父分类ID无需前端传递，默认创建一级分类
     *
     * @param createDTO 分类创建参数
     * @return 操作结果
     */
    @PostMapping("/create")
    public BaseResponse<String> createProductCategory(@RequestBody ProductCategoryCreateDTO createDTO) {
        // 调用服务层创建分类
        productCategoryService.createProductCategory(createDTO);
        log.info("商品分类创建成功，分类名称：{}", createDTO.getName());
        return ResultUtils.success("商品分类创建成功");
    }

    /**
     * 启用/禁用商品分类
     *
     * @param categoryId 分类ID
     * @param status     状态：0-禁用，1-启用
     * @return 操作结果
     */
    @GetMapping("/updateStatus/{categoryId}/{status}")
    public BaseResponse<String> updateCategoryStatus(@PathVariable Long categoryId, @PathVariable Integer status) {
        productCategoryService.updateCategoryStatus(categoryId, status);
        String action = status == 1 ? "启用" : "禁用";
        log.info("商品分类{}成功，分类ID：{}", action, categoryId);
        return ResultUtils.success("商品分类" + action + "成功");
    }

    /**
     * 删除商品分类（逻辑删除）
     *
     * @param categoryId 分类ID
     * @return 操作结果
     */
    @GetMapping("/delete/{categoryId}")
    public BaseResponse<String> deleteProductCategory(@PathVariable Long categoryId) {
        productCategoryService.deleteProductCategory(categoryId);
        log.info("商品分类删除成功，分类ID：{}", categoryId);
        return ResultUtils.success("商品分类删除成功");
    }

    /**
     * 修改商品分类信息
     * 支持修改：名称、图标、描述、排序权重、状态
     *
     * @param updateDTO 修改参数（包含分类ID和待修改的字段）
     * @return 操作结果
     */
    @PostMapping("/update")
    public BaseResponse<String> updateProductCategory(@RequestBody ProductCategoryUpdateDTO updateDTO) {
        // 执行修改操作
        productCategoryService.updateProductCategory(updateDTO);

        log.info("商品分类修改成功，分类ID：{}", updateDTO.getId());
        return ResultUtils.success("商品分类修改成功");
    }

    /**
     * 查询商品分类列表
     * 支持按名称（模糊查询）和状态（精确查询）筛选，参数全不传则返回所有分类
     *
     * @param name   分类名称（可选，模糊查询）
     * @param status 分类状态（可选，0-禁用，1-启用）
     * @return 分类列表
     */
    @GetMapping("/list")
    public BaseResponse<List<ProductCategoryVO>> getCategoryList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        // 执行查询
        List<ProductCategoryVO> categoryList = productCategoryService.getCategoryList(name, status);
        return ResultUtils.success(categoryList);
    }

    /**
     * 根据分类ID查询旗下商品列表
     *
     * @param categoryId 分类ID（必填）
     * @return 该分类下的商品列表
     */
    @GetMapping("/products/{categoryId}")
    public BaseResponse<List<ProductVO>> getProductsByCategoryId(@PathVariable Long categoryId) {
        // 执行查询：查询该分类下未删除、已上架的商品
        List<ProductVO> productList = productCategoryService.getProductsByCategoryId(categoryId);
        return ResultUtils.success(productList);
    }

    /**
     * 分页查询商品分类信息
     *
     * @param queryDTO 分页及查询条件参数
     * @return 分页结果
     */
    @PostMapping("/pageQuery")
    public BaseResponse<IPage<ProductCategoryVO>> pageQueryProductCategories(@RequestBody ProductCategoryPageQueryDTO queryDTO) {
        IPage<ProductCategoryVO> pageResult = productCategoryService.pageQueryProductCategories(queryDTO);
        log.info("分页查询商品分类信息，参数：{}，结果：{}", queryDTO, pageResult);
        return ResultUtils.success(pageResult);
    }

}
