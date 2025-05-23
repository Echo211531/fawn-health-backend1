package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.food.FoodAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryUpdateDTO;
import com.ljh.fawnhealth.model.dto.food.FoodUpdateDTO;
import com.ljh.fawnhealth.model.vo.food.FoodCategoryVO;
import com.ljh.fawnhealth.model.vo.food.FoodLibraryVO;
import com.ljh.fawnhealth.service.FoodCategoriesService;
import com.ljh.fawnhealth.service.FoodLibraryService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/food")
public class FoodLibraryController {

    @Resource
    private  FoodLibraryService foodLibraryService;

    @Resource
    private FoodCategoriesService foodCategoriesService;

    /**
     * 获取食物分类列表
     *
     * @return 食物分类列表（VO）
     */
    @GetMapping("/foodCategoriesList")
    public BaseResponse<List<FoodCategoryVO>> getFoodCategoriesList() {
        List<FoodCategoryVO> foodCategoryVOList = foodCategoriesService.getFoodCategoriesList();
        log.info("获取食物分类列表：{}",foodCategoryVOList);
        return ResultUtils.success(foodCategoryVOList);
    }

    /**
     * 添加食物
     *
     * @param foodAddDTO 添加食物DTO
     * @return foodLibraryVO
     */
    @PostMapping("/addFood")
    public BaseResponse<FoodLibraryVO> addFood(@RequestBody FoodAddDTO foodAddDTO) {
        log.info("添加食物: {}", foodAddDTO.getName());
        FoodLibraryVO foodLibraryVO = foodLibraryService.addFood(foodAddDTO);
        return ResultUtils.success(foodLibraryVO);
    }

    /**
     * 修改食物
     *
     * @param foodUpdateDTO  修改食物DTO
     * @return foodLibraryVO
     */
    @PostMapping("/update")
    public BaseResponse<FoodLibraryVO> updateFood(@RequestBody FoodUpdateDTO foodUpdateDTO) {
        log.info("更新食物: {}", foodUpdateDTO.getName());
        FoodLibraryVO foodLibraryVO = foodLibraryService.updateFood(foodUpdateDTO);
        return ResultUtils.success(foodLibraryVO);
    }

    /**
     * 删除食物
     *
     * @param foodId 食物 ID
     * @return 删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<String> deleteFood(@RequestParam String foodId) {
        log.warn("删除食物: {}", foodId);
        foodLibraryService.deleteFood(foodId);
        return ResultUtils.success("删除成功");
    }

    /**
     * 根据分类 ID 获取该分类下的食物列表
     *
     * @param categoryId 分类 ID
     * @return 食物列表
     */
    @GetMapping("/listByCategory")
    public BaseResponse<List<FoodLibraryVO>> getFoodsByCategoryId(@RequestParam Long categoryId) {
        log.info("查询分类下的食物，分类ID: {}", categoryId);
        List<FoodLibraryVO> foodList = foodLibraryService.getFoodsByCategoryId(categoryId);
        return ResultUtils.success(foodList);
    }

    /**
     * 根据食物 ID 获取完整的食物信息
     *
     * @param foodId 食物 ID
     * @return 食物信息 VO
     */
    @GetMapping("/getFoodDetail")
    public BaseResponse<FoodLibraryVO> getFoodDetail(@RequestParam Long foodId) {
        log.info("查询食物详情，ID: {}", foodId);
        FoodLibraryVO foodDetail = foodLibraryService.getFoodDetailById(foodId);
        return ResultUtils.success(foodDetail);
    }

    /**
     * 添加食物分类
     *
     * @param foodCategoryAddDTO 分类信息
     * @return 添加后的分类信息
     */
    @PostMapping("/addCategory")
    public BaseResponse<FoodCategoryVO> addFoodCategory(@RequestBody FoodCategoryAddDTO foodCategoryAddDTO) {
        log.info("添加食物分类: {}", foodCategoryAddDTO.getName());
        FoodCategoryVO result = foodCategoriesService.addFoodCategory(foodCategoryAddDTO);
        return ResultUtils.success(result);
    }

    /**
     * 修改食物分类
     *
     * @param foodCategoryUpdateDTO 分类信息
     * @return 修改后的分类信息
     */
    @PostMapping("/updateCategory")
    public BaseResponse<FoodCategoryVO> updateFoodCategory(@RequestBody FoodCategoryUpdateDTO foodCategoryUpdateDTO) {
        log.info("修改食物分类: ID={}, 名称={}", foodCategoryUpdateDTO.getId(), foodCategoryUpdateDTO.getName());
        FoodCategoryVO result = foodCategoriesService.updateFoodCategory(foodCategoryUpdateDTO);
        return ResultUtils.success(result);
    }

    /**
     * 删除食物分类
     *
     * @param categoryId 分类 ID
     * @return 删除成功
     */
    @PostMapping("/deleteCategory")
    public BaseResponse<String> deleteFoodCategory(@RequestParam Long categoryId) {
        log.warn("删除食物分类: {}", categoryId);
        foodCategoriesService.deleteFoodCategory(categoryId);
        return ResultUtils.success("删除成功");
    }



//
//    @GetMapping("/search")
//    public BaseResponse<FoodSearchResultDTO> searchFood(
//            @RequestParam(required = false) String keyword,
//            @RequestParam(required = false) Integer category_id,
//            @RequestParam(defaultValue = "1") int pageNum,
//            @RequestParam(defaultValue = "10") int pageSize
//    ) {
//        return ResultUtils.success(foodLibraryService.searchFood(keyword, category_id, pageNum, pageSize));
//    }
}
