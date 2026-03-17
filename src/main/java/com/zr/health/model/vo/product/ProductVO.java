package com.zr.health.model.vo.product;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品视图对象
 */
@Data
public class ProductVO {
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
     * 销量
     */
    private Integer sales;

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
    private Object specs;

    /**
     * 状态：0-下架，1-上架，2-缺货
     */
    private Integer status;

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

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}