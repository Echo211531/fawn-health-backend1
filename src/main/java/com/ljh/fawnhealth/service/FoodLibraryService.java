package com.ljh.fawnhealth.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ljh.fawnhealth.model.dto.food.FoodAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodUpdateDTO;
import com.ljh.fawnhealth.model.entity.FoodLibrary;
import com.ljh.fawnhealth.model.vo.food.FoodLibraryVO;

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
}
