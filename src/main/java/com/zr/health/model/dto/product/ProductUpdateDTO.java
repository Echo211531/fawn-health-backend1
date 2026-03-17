package com.zr.health.model.dto.product;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 商品修改参数DTO
 * 用于接收前端传递的商品修改信息，包含需要更新的商品字段
 */
@Data
public class ProductUpdateDTO {

    /**
     * 商品ID
     */
    private Long id;

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
     * 子图URL
     */
    private String subImages;

    /**
     * 商品详情
     */
    private String detail;

    /**
     * 商品规格
     * JSON格式的字符串，例如：{"flavor":"巧克力","weight":"500g"}
     * 用于存储商品的具体规格参数
     */
    private String specs;

    /**
     * 商品重量(g)
     * 商品实际重量，单位为克，用于计算运费等场景
     */
    private BigDecimal weight;

    /**
     * 是否热销
     * 0-否，1-是；用于首页、分类页等场景的热销商品展示
     */
    private Integer isHot;

    /**
     * 是否推荐
     * 0-否，1-是；用于首页推荐、猜你喜欢等场景的商品展示
     */
    private Integer isRecommend;

    /**
     * 排序权重
     * 数值越大排序越靠前，用于商品列表的排序展示
     */
    private Integer sortOrder;
}