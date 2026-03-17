package com.zr.health.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zr.health.commen.BaseResponse;
import com.zr.health.config.ResultUtils;
import com.zr.health.model.dto.food.*;
import com.zr.health.model.vo.food.FoodCategoryVO;
import com.zr.health.model.vo.food.FoodLibraryVO;
import com.zr.health.service.FoodCategoriesService;
import com.zr.health.service.FoodLibraryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 食物分类模块
 * 提供食物分类的增删改查等功能接口
 */
@Slf4j
@RestController
@RequestMapping("/foodLibrary")
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

    /**
     * 获取常见食物信息列表（不分页）
     *
     * @param categoryId 分类ID，可选
     * @return 常见食物信息列表
     */
    @GetMapping("/commonFoods")
    public BaseResponse<List<FoodLibraryVO>> getCommonFoods(
            @RequestParam(required = false) Long categoryId) {
        log.info("获取常见食物列表，分类ID: {}", categoryId);
        List<FoodLibraryVO> commonFoods = foodLibraryService.getCommonFoods(categoryId);
        return ResultUtils.success(commonFoods);
    }

    /**
     * 根据食物名称模糊搜索（不分页，返回所有匹配结果）
     * @param keyword 食物名称关键词（前端只需传这个参数）
     * @return 匹配的食物列表
     */
    @GetMapping("/searchByName")
    public BaseResponse<List<FoodLibraryVO>> searchFoodByName(@RequestParam String keyword) {
        log.info("模糊搜索食物，关键词：{}", keyword);
        List<FoodLibraryVO> foodList = foodLibraryService.searchFoodByName(keyword.trim());
        return ResultUtils.success(foodList);
    }

    /**
     * 分页查询食物分类列表
     *
     * @param queryDTO
     * @return
     */
    @PostMapping("/category/pageQuery")
    public BaseResponse<IPage<FoodCategoryVO>> pageQueryFoodCategories(@RequestBody FoodCategoryPageQueryDTO queryDTO) {
        log.info("分页查询食物分类，参数：{}", queryDTO);
        IPage<FoodCategoryVO> pageResult = foodCategoriesService.pageQueryFoodCategories(queryDTO);
        return ResultUtils.success(pageResult);
    }

    /**
     * 分页查询食物信息（支持多条件筛选）
     *
     * @param queryDTO 分页及查询条件参数
     * @return 分页结果（包含食物列表及分页信息）
     */
    @PostMapping("/pageQuery")
    public BaseResponse<IPage<FoodLibraryVO>> pageQueryFoods(@RequestBody FoodPageQueryDTO queryDTO) {
        log.info("分页查询食物信息，参数：{}", queryDTO);
        IPage<FoodLibraryVO> pageResult = foodLibraryService.pageQueryFoods(queryDTO);
        return ResultUtils.success(pageResult);
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
