package com.zr.health.model.dto.food;

import lombok.Data;

/**
 * 食物分类修改数据传输对象（DTO），用于封装在修改食物分类到食物分类库时所需的信息。。
 */
@Data
public class FoodCategoryUpdateDTO {

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
     * 状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重
     */
    private Integer sortOrder;

}
