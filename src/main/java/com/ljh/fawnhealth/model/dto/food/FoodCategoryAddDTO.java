package com.ljh.fawnhealth.model.dto.food;

import lombok.Data;

/**
 * 食物分类添加数据传输对象（DTO），用于封装在添加新食物分类到食物分类库时所需的信息。。
 */
@Data
public class FoodCategoryAddDTO {

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

}
