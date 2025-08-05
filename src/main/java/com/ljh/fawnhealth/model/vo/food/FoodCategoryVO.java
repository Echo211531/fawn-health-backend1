package com.ljh.fawnhealth.model.vo.food;

import lombok.Data;

import java.util.Date;


/**
 * 食物分类视图对象（VO），用于在展示层展示食物分类的相关信息。
 */
@Data
public class FoodCategoryVO {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

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
