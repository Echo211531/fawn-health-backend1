package com.ljh.fawnhealth.controller;

import com.alibaba.nacos.api.model.v2.Result;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.model.dto.product.ProductCreateDTO;
import com.ljh.fawnhealth.model.dto.product.ProductListQueryDTO;
import com.ljh.fawnhealth.model.dto.product.ProductUpdateDTO;
import com.ljh.fawnhealth.model.dto.user.UserLoginDTO;
import com.ljh.fawnhealth.model.dto.user.UserUpdateDTO;
import com.ljh.fawnhealth.model.dto.user.WeightDTO;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.product.ProductVO;
import com.ljh.fawnhealth.model.vo.user.UserInfoVO;
import com.ljh.fawnhealth.model.vo.user.UserLoginVO;
import com.ljh.fawnhealth.service.EmailService;
import com.ljh.fawnhealth.service.ProductService;
import com.ljh.fawnhealth.service.UserService;
import com.ljh.fawnhealth.utils.CharUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
}
