package com.ljh.fawnhealth.model.vo.product;

import lombok.Data;
import java.util.Date;

/**
 * 商品分类VO（返回给前端的视图对象）
 */
@Data
public class ProductCategoryVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID（默认0，一级分类）
     */
    private Long parentId;

    /**
     * 分类层级（默认1，一级分类）
     */
    private Integer level;

    /**
     * 分类图标
     */
    private String icon;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重
     */
    private Integer sortOrder;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}