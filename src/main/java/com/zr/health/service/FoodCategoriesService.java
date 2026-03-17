package com.zr.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.food.FoodCategoryAddDTO;
import com.zr.health.model.dto.food.FoodCategoryPageQueryDTO;
import com.zr.health.model.dto.food.FoodCategoryUpdateDTO;
import com.zr.health.model.entity.FoodCategories;
import com.zr.health.model.vo.food.FoodCategoryVO;
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

    /**
     * 分页查询食物分类列表
     *
     * @param queryDTO
     * @return
     */
    IPage<FoodCategoryVO> pageQueryFoodCategories(FoodCategoryPageQueryDTO queryDTO);
}
