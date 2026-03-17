package com.zr.health.model.dto.product;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品创建数据传输对象
 */
@Data
public class ProductCreateDTO {
    /**
     * 商品名称
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 主图URL
     */
    private String mainImage;

    /**
     * 子图URL，多个用逗号分隔
     */
    private String subImages;

    /**
     * 商品详情
     */
    private String detail;

    /**
     * 商品规格，JSON格式
     */
    private String specs;

    /**
     * 商品重量(g)
     */
    private BigDecimal weight;

    /**
     * 是否热销：0-否，1-是
     */
    private Integer isHot;

    /**
     * 是否推荐：0-否，1-是
     */
    private Integer isRecommend;
}