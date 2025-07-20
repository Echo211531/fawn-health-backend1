package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.mapper.OrderMapper;
import com.ljh.fawnhealth.mapper.ProductMapper;
import com.ljh.fawnhealth.model.dto.product.ProductCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductListQueryDTO;
import com.ljh.fawnhealth.model.dto.product.ProductUpdateDTO;
import com.ljh.fawnhealth.model.entity.Product;
import com.ljh.fawnhealth.model.entity.ProductCategory;
import com.ljh.fawnhealth.model.vo.product.ProductVO;
import com.ljh.fawnhealth.service.ProductService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author 27105
* @description 针对表【product(商品表)】的数据库操作Service实现
* @createDate 2025-07-14 22:59:34
*/
@Slf4j
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements ProductService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderMapper orderMapper;

    /**
     * 管理员创建商品
     *
     * @param dto 商品创建参数
     * @return 操作结果
     */
    @Override
    public void createProduct(ProductCreateDTO dto) {
        // 检查商品名称是否已存在
        if (productMapper.selectByProductName(dto.getName())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"商品名称已存在");
        }

        // DTO转Entity
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(0); // 默认下架状态
        product.setSales(0);
        product.setIsDelete(0);
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());

        // 保存商品
        productMapper.insert(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFoodStatus(Long productId, Integer status) {
        // 1. 参数校验
        if (productId == null || status == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID和状态不能为空");
        }
        if (status < 0 || status > 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=下架，1=上架，2=缺货");
        }

        // 2. 查询商品信息（过滤已删除商品）
        Product product = productMapper.selectByIdAndNotDelete(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在或已删除");
        }

        // 3. 相同状态无需处理（幂等性保障）
        if (product.getStatus().equals(status)) {
            log.info("商品状态未变更，无需更新，商品ID: {}", productId);
            return;
        }

        // 4. 状态转换业务规则校验
        validateStatusTransition(product.getStatus(), status, product.getStock());

        // 5. 构建更新对象
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setStatus(status);

        // 6. 执行更新
        int rows = productMapper.updateById(updateProduct);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品状态更新失败");
        }

        // 7. 记录操作日志
        String action = getStatusAction(status);
        log.info("商品状态更新成功，商品ID: {}, 操作: {}", productId, action);

        // 8. 如果是缺货状态且库存为0，可在这里添加库存预警通知逻辑
        if (status == 2 && product.getStock() <= 0) {
            sendStockWarningNotification(product);
        }
    }

    /**
     * 修改商品信息（DTO中包含商品ID）
     *
     * @param updateDTO 商品修改参数（包含商品ID）
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(ProductUpdateDTO updateDTO) {

        // 1. 校验参数合法性
        if (updateDTO == null || updateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不能为空");
        }

        // 2. 查询商品是否存在且未被删除
        Product product = productMapper.selectByIdAndNotDelete(updateDTO.getId());
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在或已删除");
        }

        // 3. 构建更新对象（只更新非空字段）
        Product updateProduct = new Product();
        updateProduct.setId(updateDTO.getId());

        // 商品名称（非空校验）
        if (StringUtils.hasText(updateDTO.getName())) {
            if (updateDTO.getName().length() > 100) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品名称长度不能超过100字符");
            }
            updateProduct.setName(updateDTO.getName());
        }

        updateProduct.setCategoryId(updateDTO.getCategoryId());

        // 商品描述
        if (updateDTO.getDescription() != null) {
            updateProduct.setDescription(updateDTO.getDescription());
        }

        // 商品价格（需大于0）
        if (updateDTO.getPrice() != null) {
            if (updateDTO.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品价格必须大于0");
            }
            updateProduct.setPrice(updateDTO.getPrice());
        }

        // 原价（需大于等于0）
        if (updateDTO.getOriginalPrice() != null) {
            if (updateDTO.getOriginalPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品原价不能小于0");
            }
            updateProduct.setOriginalPrice(updateDTO.getOriginalPrice());
        }

        // 库存数量（需大于等于0，且更新后处理状态联动）
        if (updateDTO.getStock() != null) {
            if (updateDTO.getStock() < 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "库存数量不能小于0");
            }
            updateProduct.setStock(updateDTO.getStock());

            // 库存为0时自动标记为缺货（当前状态为上架时）
            if (updateDTO.getStock() == 0 && product.getStatus() == 1) {
                updateProduct.setStatus(2); // 2-缺货
            }
        }

        // 主图URL
        if (StringUtils.hasText(updateDTO.getMainImage())) {
            updateProduct.setMainImage(updateDTO.getMainImage());
        }

        // 子图URL（校验格式）
        if (updateDTO.getSubImages() != null) {
            if (StringUtils.hasText(updateDTO.getSubImages())) {
                String[] subImageArray = updateDTO.getSubImages().split(",");
                if (subImageArray.length > 5) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "子图数量不能超过5张");
                }
            }
            updateProduct.setSubImages(updateDTO.getSubImages());
        }

        // 商品详情
        if (updateDTO.getDetail() != null) {
            updateProduct.setDetail(updateDTO.getDetail());
        }


        // 商品重量（需大于0）
        if (updateDTO.getWeight() != null) {
            if (updateDTO.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品重量必须大于0");
            }
            updateProduct.setWeight(updateDTO.getWeight());
        }

        // 是否热销（只能是0或1）
        if (updateDTO.getIsHot() != null) {
            if (updateDTO.getIsHot() != 0 && updateDTO.getIsHot() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否热销只能是0或1");
            }
            updateProduct.setIsHot(updateDTO.getIsHot());
        }

        // 是否推荐（只能是0或1）
        if (updateDTO.getIsRecommend() != null) {
            if (updateDTO.getIsRecommend() != 0 && updateDTO.getIsRecommend() != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否推荐只能是0或1");
            }
            updateProduct.setIsRecommend(updateDTO.getIsRecommend());
        }

        // 排序权重
        if (updateDTO.getSortOrder() != null) {
            updateProduct.setSortOrder(updateDTO.getSortOrder());
        }

        // 4. 执行更新操作
        int rows = productMapper.updateById(updateProduct);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品信息更新失败");
        }

        // 5. 记录操作日志
        log.info("商品信息更新成功，商品ID: {}", updateDTO.getId());
    }

    /**
     * 逻辑删除商品
     *
     * @param productId 商品ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(Long productId) {
        // 参数校验
        if (productId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不能为空");
        }
        // 1. 校验商品是否存在且未被删除
        Product product = productMapper.selectByIdAndNotDelete(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在或已删除");
        }

        // 2. 检查是否有相关订单（防止删除有交易记录的商品）
        int relatedOrderCount = orderMapper.countByProductId(productId);
        if (relatedOrderCount > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "该商品存在关联订单，无法删除");
        }

        // 3. 执行逻辑删除（更新is_delete为1）
        Product updateProduct = new Product();
        updateProduct.setId(productId);
        updateProduct.setIsDelete(1); // 1表示已删除

        int rows = productMapper.updateById(updateProduct);
        if (rows <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "商品删除失败");
        }

        log.info("商品逻辑删除成功，商品ID: {}", productId);
    }

    /**
     * 根据多条件查询商品列表
     *
     * @param name 商品名称（模糊）
     * @param status 状态
     * @param isHot 是否热销
     * @param isRecommend 是否推荐
     * @return
     */
    @Override
    public List<ProductVO> getProductListByParams(String name, Integer status, Integer isHot, Integer isRecommend) {
        // 构建查询条件
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_delete", 0); // 过滤已删除商品

        // 商品名称（模糊查询，参数为空则不添加该条件）
        if (StringUtils.hasText(name)) {
            queryWrapper.like("name", name);
        }

        // 状态（参数为空则不添加该条件，非空则校验合法性）
        if (status != null) {
            if (status != 0 && status != 1 && status != 2) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态参数错误，0=下架，1=上架，2=缺货");
            }
            queryWrapper.eq("status", status);
        }

        // 是否热销（参数为空则不添加该条件）
        if (isHot != null) {
            if (isHot != 0 && isHot != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否热销参数错误，0=否，1=是");
            }
            queryWrapper.eq("is_hot", isHot);
        }

        // 是否推荐（参数为空则不添加该条件）
        if (isRecommend != null) {
            if (isRecommend != 0 && isRecommend != 1) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "是否推荐参数错误，0=否，1=是");
            }
            queryWrapper.eq("is_recommend", isRecommend);
        }

        // 默认按排序权重降序
        queryWrapper.orderByDesc("sort_order");

        // 执行查询
        List<Product> productList = productMapper.selectList(queryWrapper);

        // 转换为VO
        return productList.stream().map(product -> {
            ProductVO productVO = new ProductVO();
            BeanUtils.copyProperties(product, productVO);
            return productVO;
        }).collect(Collectors.toList());
    }

    /**
     * 根据商品ID查询商品详细信息
     *
     * @param productId 商品ID
     * @return 商品详细信息（ProductVO）
     */
    @Override
    public ProductVO getProductById(Long productId) {

        // 参数校验
        if (productId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不能为空");
        }

        // 查询商品实体（只查询未删除的商品）
        Product product = productMapper.selectOne(
                new QueryWrapper<Product>()
                        .eq("id", productId)
                        .eq("is_delete", 0)
        );

        if (product == null) {
            return null; // 商品不存在或已删除
        }

        // 转换为VO并返回（补充完整信息）
        ProductVO productVO = new ProductVO();
        BeanUtils.copyProperties(product, productVO);

//        // 可选：补充关联信息（如分类名称，需关联查询分类表）
//        if (product.getCategoryId() != null) {
//            ProductCategory category = productCategoryMapper.selectById(product.getCategoryId());
//            if (category != null) {
//                productVO.setCategoryName(category.getName()); // 假设ProductVO中有categoryName字段
//            }
//        }

        return productVO;
    }

    /**
     * 查询推荐商品列表
     * 只查询状态为上架（status=1）且标记为推荐（is_recommend=1）的商品
     *
     * @return 推荐商品列表
     */
    /**
     * 获取推荐商品列表
     * 只查询状态为上架（status=1）且标记为推荐（is_recommend=1）的商品
     * 按sort_order排序，权重高的排在前面
     */
    @Override
    public List<ProductVO> getRecommendProducts() {
        // 构建查询条件
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)  // 只查询上架商品
                .eq("is_recommend", 1)  // 只查询推荐商品
                .eq("is_delete", 0)  // 排除已删除商品
                .orderByDesc("sort_order")  // 按排序权重降序排列
                .last("limit 20");  // 限制最多返回10条推荐商品

        List<Product> productList = productMapper.selectList(queryWrapper);

        // 转换为VO并返回
        return productList.stream()
                .map(product -> {
                    ProductVO productVO = new ProductVO();
                    BeanUtils.copyProperties(product, productVO);
                    return productVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据商品名称模糊查询商品分类信息
     * 只查询状态为上架（status=1）且未删除（is_delete=0）的商品
     *
     * @param name 商品名称（模糊匹配）
     * @return 符合条件的商品分类信息列表
     */
    @Override
    public List<ProductVO> searchProductsByCategory(String name) {
        QueryWrapper<Product> queryWrapper = new QueryWrapper<>();
        // 模糊匹配商品名称
        queryWrapper.like("name", name)
                // 只查询上架商品
                .eq("status", 1)
                // 排除已删除商品
                .eq("is_delete", 0)
                // 按分类排序
                .orderByAsc("category_id");

        List<Product> products = productMapper.selectList(queryWrapper);

        return products.stream()
                .map(product -> {
                    ProductVO productVO = new ProductVO();
                    BeanUtils.copyProperties(product, productVO);
                    return productVO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 校验状态转换的业务规则
     */
    private void validateStatusTransition(Integer currentStatus, Integer newStatus, Integer stock) {
        // 上架操作校验：库存必须大于0才能上架
        if (newStatus == 1) { // 1-上架
            if (stock <= 0) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存不足，无法上架商品");
            }
        }

        // 缺货操作校验：只有上架状态才能转为缺货状态
        if (newStatus == 2) { // 2-缺货
            if (currentStatus != 1) { // 1-上架
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有上架状态的商品才能标记为缺货");
            }
            if (stock > 0) {
                log.warn("商品库存不为0却标记为缺货状态，商品库存: {}", stock);
                // 这里可以选择抛出异常或仅警告，根据业务需求决定
                // throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存不为0，不能标记为缺货");
            }
        }

        // 下架操作没有特殊限制，任何状态都能转为下架
    }

    /**
     * 获取状态操作描述
     */
    private String getStatusAction(Integer status) {
        switch (status) {
            case 0: return "下架";
            case 1: return "上架";
            case 2: return "标记为缺货";
            default: return "未知操作";
        }
    }

    /**
     * 发送库存预警通知
     */
    private void sendStockWarningNotification(Product product) {
        // 这里实现库存预警通知逻辑，例如发送邮件、短信给管理员
        try {
            String message = String.format("商品【%s】(ID: %s)已缺货，请及时补货，当前库存: %d",
                    product.getName(), product.getId(), product.getStock());
            // emailService.sendStockWarningEmail(message);
            log.info("库存预警通知: {}", message);
        } catch (Exception e) {
            log.error("发送库存预警通知失败", e);
            // 通知失败不影响主流程，仅记录日志
        }
    }
}




