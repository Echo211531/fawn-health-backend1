package com.zr.health.service;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zr.health.model.dto.food.FoodAddDTO;
import com.zr.health.model.dto.food.FoodPageQueryDTO;
import com.zr.health.model.dto.food.FoodUpdateDTO;
import com.zr.health.model.entity.FoodLibrary;
import com.zr.health.model.vo.food.FoodLibraryVO;

import java.util.List;

/**
 * 食物相关服务接口
 */
public interface FoodLibraryService extends IService<FoodLibrary> {

    /**
     * 添加食物
     * @param foodAddDTO 添加食物DTO
     * @return FoodLibraryVO
     */
    FoodLibraryVO addFood(FoodAddDTO foodAddDTO);

    /**
     * 修改食物
     * @param foodUpdateDTO 修改食物DTO
     * @return FoodLibraryVO
     */
    FoodLibraryVO updateFood(FoodUpdateDTO foodUpdateDTO);

    /**
     * 删除食物
     * @param foodId 食物 ID
     */
    void deleteFood(String foodId);

    /**
     * 根据分类 ID 获取该分类下的食物列表
     * @param categoryId 分类 ID
     * @return List<FoodLibraryVO>
     */
    List<FoodLibraryVO> getFoodsByCategoryId(Long categoryId);

    /**
     * 根据食物 ID 获取完整的食物信息
     * @param foodId 食物 ID
     * @return FoodLibraryVO
     */
    FoodLibraryVO getFoodDetailById(Long foodId);

    /**
     * 获取常见食物信息列表（不分页）
     *
     * @param categoryId 分类ID，可选
     * @return 常见食物信息列表
     */
    List<FoodLibraryVO> getCommonFoods(Long categoryId);

    /**
     * 根据食物名称模糊搜索（不分页，返回所有匹配结果）
     * @param keyword 食物名称关键词（前端只需传这个参数）
     * @return 匹配的食物列表
     */
    List<FoodLibraryVO> searchFoodByName(String keyword);

    /**
     * 分页查询食物信息（支持多条件筛选）
     *
     * @param queryDTO 分页及查询条件参数
     * @return 分页结果（包含食物列表及分页信息）
     */
    IPage<FoodLibraryVO> pageQueryFoods(FoodPageQueryDTO queryDTO);
}
