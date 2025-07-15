package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.ProductCategoryMapper;
import com.ljh.fawnhealth.mapper.ProductMapper;
import com.ljh.fawnhealth.model.dto.product.ProductCategoryCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductCategoryUpdateDTO;
import com.ljh.fawnhealth.model.entity.Product;
import com.ljh.fawnhealth.model.entity.ProductCategory;
import com.ljh.fawnhealth.model.vo.product.ProductCategoryVO;
import com.ljh.fawnhealth.model.vo.product.ProductVO;
import com.ljh.fawnhealth.service.ProductCategoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【product_category(商品分类表)】的数据库操作Service实现
* @createDate 2025-07-14 23:00:12
*/
@Slf4j
@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory>
    implements ProductCategoryService {

    @Resource
    private ProductCategoryMapper productCategoryMapper;

    @Resource
    private ProductMapper productMapper;

    /**
     * 创建商品分类
     * 分类层级和父分类ID无需前端传递，默认创建一级分类
     *
     * @param createDTO 分类创建参数
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createProductCategory(ProductCategoryCreateDTO createDTO) {
        // 1. 校验状态参数合法性
        if (createDTO.getStatus() != 0 && createDTO.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=禁用，1=启用");
        }

        // 2. 校验排序权重合理性（避免过大或负数）
        if (createDTO.getSortOrder() < 0 || createDTO.getSortOrder() > 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序权重必须在0-1000之间");
        }

        // 3. 检查分类名称是否已存在（同一层级不允许重名）
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", createDTO.getName())
                .eq("parent_id", 0)  // 只检查一级分类
                .eq("is_delete", 0);
        Long count = productCategoryMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该分类名称已存在");
        }

        // 4. 构建分类实体（自动设置父ID和层级为一级分类）
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(createDTO, category);
        category.setParentId(0L);  // 父分类ID默认为0（一级分类）
        category.setLevel(1);      // 分类层级默认为1（一级分类）

        // 5. 执行创建操作
        int rows = productCategoryMapper.insert(category);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分类创建失败");
        }
    }

    /**
     * 启用/禁用商品分类
     *
     * @param categoryId 分类ID
     * @param status     状态：0-禁用，1-启用
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategoryStatus(Long categoryId, Integer status) {
        // 校验状态参数
        if (status != 0 && status != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=禁用，1=启用");
        }
        // 校验分类ID
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        // 1. 检查分类是否存在且未被删除
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null || category.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品分类不存在或已删除");
        }

        // 2. 检查是否需要更新（状态未变化则无需操作）
        if (category.getStatus().equals(status)) {
            log.info("商品分类状态未变化，分类ID：{}，当前状态：{}", categoryId, status);
            return;
        }

        // 3. 若要禁用分类，先先检查该分类下是否有商品
        if (status == 0) {
            // 3.1 检查当前分类下是否有商品（未删除的）
            QueryWrapper<Product> productWrapper = new QueryWrapper<>();
            productWrapper.eq("category_id", categoryId)
                    .eq("is_delete", 0);
            Long productCount = productMapper.selectCount(productWrapper);
            if (productCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "该分类下存在" + productCount + "个商品，无法禁用，请先转移或删除商品");
            }

            // 3.2 检查是否有子分类（如果有子分类也不允许禁用，可选逻辑）
            QueryWrapper<ProductCategory> subCategoryWrapper = new QueryWrapper<>();
            subCategoryWrapper.eq("parent_id", categoryId)
                    .eq("is_delete", 0);
            Long subCategoryCount = productCategoryMapper.selectCount(subCategoryWrapper);
            if (subCategoryCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "该分类下存在" + subCategoryCount + "个子分类，无法禁用，请先处理子分类");
            }
        }

        // 4. 执行状态更新
        ProductCategory updateCategory = new ProductCategory();
        updateCategory.setId(categoryId);
        updateCategory.setStatus(status);

        int rows = productCategoryMapper.updateById(updateCategory);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分类状态更新失败");
        }

        log.info("商品分类状态更新成功，分类ID：{}，新状态：{}", categoryId, status);
    }

    /**
     * 删除商品分类（逻辑删除）
     *
     * @param categoryId 分类ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductCategory(Long categoryId) {
        // 参数校验
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        // 1. 检查分类是否存在且未被删除
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null || category.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品分类不存在或已删除");
        }

        // 2. 检查分类下是否有商品（有商品则不允许删除）
        QueryWrapper<Product> productWrapper = new QueryWrapper<>();
        productWrapper.eq("category_id", categoryId)
                .eq("is_delete", 0);
        Long productCount = productMapper.selectCount(productWrapper);
        if (productCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "该分类下存在" + productCount + "个商品，无法删除，请先转移或删除商品");
        }

        // 3. 检查是否有子分类（有子分类则不允许删除）
        QueryWrapper<ProductCategory> subCategoryWrapper = new QueryWrapper<>();
        subCategoryWrapper.eq("parent_id", categoryId)
                .eq("is_delete", 0);
        Long subCategoryCount = productCategoryMapper.selectCount(subCategoryWrapper);
        if (subCategoryCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,
                    "该分类下存在" + subCategoryCount + "个子分类，无法删除，请先删除子分类");
        }

        // 4. 执行逻辑删除（更新is_delete为1）
        ProductCategory updateCategory = new ProductCategory();
        updateCategory.setId(categoryId);
        updateCategory.setIsDelete(1);

        int rows = productCategoryMapper.updateById(updateCategory);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分类删除失败");
        }
    }

    /**
     * 修改商品分类信息
     * 支持修改：名称、图标、描述、排序权重、状态
     *
     * @param updateDTO 修改参数（包含分类ID和待修改的字段）
     * @return 操作结果
     */
    @Override
    public void updateProductCategory(ProductCategoryUpdateDTO updateDTO) {
        // 参数校验（ID不能为空）
        if (updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        // 校验状态参数合法性
        if (updateDTO.getStatus() != 0 && updateDTO.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=禁用，1=启用");
        }
        // 校验排序权重范围
        if (updateDTO.getSortOrder() != null && (updateDTO.getSortOrder() < 0 || updateDTO.getSortOrder() > 1000)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "排序权重必须在0-1000之间");
        }

        // 1. 检查分类是否存在且未被删除
        ProductCategory category = productCategoryMapper.selectById(updateDTO.getId());
        if (category == null || category.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品分类不存在或已删除");
        }

        // 2. 检查分类名称是否重复（排除当前分类自身）
        QueryWrapper<ProductCategory> nameWrapper = new QueryWrapper<>();
        nameWrapper.eq("name", updateDTO.getName())
                .ne("id", updateDTO.getId()) // 排除自身
                .eq("is_delete", 0);
        Long nameCount = productCategoryMapper.selectCount(nameWrapper);
        if (nameCount > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该分类名称已存在，请更换名称");
        }

        // 3. 特殊校验：如果修改后状态为禁用，需检查分类下是否有商品
        if (updateDTO.getStatus() == 0) {
            QueryWrapper<Product> productWrapper = new QueryWrapper<>();
            productWrapper.eq("category_id", updateDTO.getId())
                    .eq("is_delete", 0);
            Long productCount = productMapper.selectCount(productWrapper);
            if (productCount > 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR,
                        "该分类下存在" + productCount + "个商品，无法禁用，请先处理商品");
            }
        }

        // 4. 构建修改对象，复制可修改的字段
        ProductCategory updateCategory = new ProductCategory();
        BeanUtils.copyProperties(updateDTO, updateCategory);
        // 补充更新时间（如果表中有该字段，建议手动更新）
        updateCategory.setUpdateTime(new Date());

        // 5. 执行修改操作
        int rows = productCategoryMapper.updateById(updateCategory);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "分类信息修改失败");
        }

        log.info("商品分类修改成功，分类ID：{}", updateDTO.getId());
    }

    /**
     * 查询商品分类列表
     * 支持按名称（模糊查询）和状态（精确查询）筛选，参数全不传则返回所有分类
     *
     * @param name   分类名称（可选，模糊查询）
     * @param status 分类状态（可选，0-禁用，1-启用）
     * @return 分类列表
     */
    @Override
    public List<ProductCategoryVO> getCategoryList(String name, Integer status) {
        // 构建查询条件
        QueryWrapper<ProductCategory> queryWrapper = new QueryWrapper<>();
        // 只查询未删除的分类（逻辑删除过滤）
        queryWrapper.eq("is_delete", 0);

        // 1. 分类名称（模糊查询，参数不为空时添加条件）
        if (StringUtils.isNotBlank(name)) {
            queryWrapper.like("name", name);
        }

        // 2. 分类状态（精确查询，参数不为空时添加条件）
        if (status != null) {
            // 校验状态合法性（只允许0或1）
            if (status != 0 && status != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=禁用，1=启用");
            }
            queryWrapper.eq("status", status);
        }

        // 3. 排序：按sortOrder降序（权重高的在前），再按创建时间降序
        queryWrapper.orderByDesc("sort_order")
                .orderByDesc("create_time");

        // 4. 执行查询
        List<ProductCategory> categoryList = productCategoryMapper.selectList(queryWrapper);

        // 5. 转换为VO返回（避免暴露实体类细节）
        return categoryList.stream().map(category -> {
            ProductCategoryVO vo = new ProductCategoryVO();
            BeanUtils.copyProperties(category, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据分类ID查询旗下商品列表
     *
     * @param categoryId 分类ID（必填）
     * @return 该分类下的商品列表
     */
    @Override
    public List<ProductVO> getProductsByCategoryId(Long categoryId) {
        // 参数校验
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        }
        // 1. 先校验分类是否存在且启用
        ProductCategory category = productCategoryMapper.selectById(categoryId);
        if (category == null || category.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "分类不存在或已删除");
        }
        if (category.getStatus() == 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该分类已禁用，无法查询商品");
        }

        // 2. 查询该分类下的商品（未删除、已上架）
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId)
                .eq("is_delete", 0) // 排除已删除商品
                .eq("status", 1); // 只查询上架商品（1=上架，0=下架，2=缺货）

        // 3. 排序：按排序权重降序，再按销量降序
        queryWrapper.orderByDesc("sort_order")
                .orderByDesc("sales");

        // 4. 执行查询
        List<Product> productList = productMapper.selectList(queryWrapper);

        // 5. 转换为VO返回
        return productList.stream().map(product -> {
            ProductVO vo = new ProductVO();
            BeanUtils.copyProperties(product, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}




