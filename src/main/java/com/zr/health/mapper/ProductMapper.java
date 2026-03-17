package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.Product;


/**
* @author 27105
* @description 针对表【product(商品表)】的数据库操作Mapper
* @createDate 2025-07-14 22:59:34
* @Entity com.ljh.domain.Product
*/
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 检查商品名称是否存在
     *
     * @param name
     * @return
     */
    boolean selectByProductName(String name);

    /**
     * 根据商品ID查询商品信息
     *
     * @param productId
     * @return
     */
    Product selectByIdAndNotDelete(Long productId);
}




