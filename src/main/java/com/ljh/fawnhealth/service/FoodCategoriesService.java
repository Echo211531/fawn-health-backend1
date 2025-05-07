package com.ljh.fawnhealth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryUpdateDTO;
import com.ljh.fawnhealth.model.entity.FoodCategories;
import com.ljh.fawnhealth.model.vo.food.FoodCategoryVO;
import java.util.List;

/**
 * 食物分类相关服务接口
 */
public interface FoodCategoriesService extends IService<FoodCategories> {

    /**
     * 获取食物分类列表
     * @return 食物分类列表（VO）
     */
    List<FoodCategoryVO> getFoodCategoriesList();

    /**
     * 添加食物分类
     * @param foodCategoryAddDTO 分类信息
     * @return 添加后的分类信息
     */
    FoodCategoryVO addFoodCategory(FoodCategoryAddDTO foodCategoryAddDTO);

    /**
     * 修改食物分类
     * @param foodCategoryUpdateDTO 分类信息
     * @return 修改后的分类信息
     */
    FoodCategoryVO updateFoodCategory(FoodCategoryUpdateDTO foodCategoryUpdateDTO);

    /**
     * 删除食物分类
     * @param categoryId 分类 ID
     */
    void deleteFoodCategory(Long categoryId);

}
