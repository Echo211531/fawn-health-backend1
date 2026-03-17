package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.exception.ThrowUtils;
import com.zr.health.mapper.FoodCategoriesMapper;
import com.zr.health.mapper.FoodLibraryMapper;
import com.zr.health.model.dto.food.FoodAddDTO;
import com.zr.health.model.dto.food.FoodPageQueryDTO;
import com.zr.health.model.dto.food.FoodUpdateDTO;
import com.zr.health.model.entity.FoodCategories;
import com.zr.health.model.entity.FoodLibrary;
import com.zr.health.model.vo.food.FoodLibraryVO;
import com.zr.health.service.FoodLibraryService;
import com.zr.health.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 食物相关服务实现类
 */
@Service
public class FoodLibraryServiceImpl extends ServiceImpl<FoodLibraryMapper, FoodLibrary>
        implements FoodLibraryService {

    @Resource
    private FoodLibraryMapper foodLibraryMapper;

    @Resource
    private FoodCategoriesMapper foodCategoriesMapper;

    /**
     * 添加食物信息
     * @param foodAddDTO 添加食物所需参数
     * @return 添加成功的食物信息（VO）
     */
    @Override
    public FoodLibraryVO addFood(FoodAddDTO foodAddDTO) {

        // 参数非空校验：检查传入的 foodAddDTO 对象是否为 null，如果为 null 则抛出异常，异常码为 PARAMS_ERROR
        ThrowUtils.throwIf(foodAddDTO == null, ErrorCode.PARAMS_ERROR);

        // 从 foodAddDTO 中获取食物分类的 ID，后续用于查询对应的食物分类信息
        Long categoryId = foodAddDTO.getCategoryId();

        // 根据分类 ID 从数据库中查询对应的食物分类信息
        FoodCategories foodCategories = foodCategoriesMapper.selectById(categoryId);

        // 检查查询到的食物分类信息是否为 null，如果为 null 说明该分类 ID 对应的分类不存在
        if (foodCategories == null) {
            throw new IllegalArgumentException("分类ID对应的分类不存在");
        }

        // 使用 BeanCopyUtils 工具类将 foodAddDTO 对象的属性复制到 FoodLibrary 对象中
        FoodLibrary foodLibrary = BeanCopyUtils.copy(foodAddDTO, FoodLibrary.class);

        // 将查询到的食物分类名称设置到 FoodLibrary 对象中
        foodLibrary.setCategoryName(foodCategories.getName());
        foodLibraryMapper.insert(foodLibrary);
        return BeanCopyUtils.copy(foodLibrary, FoodLibraryVO.class);
    }

    /**
     * 修改食物
     * @param foodUpdateDTO 修改食物所需参数
     * @return 修改后的食物信息（VO）
     */
    @Override
    public FoodLibraryVO updateFood(FoodUpdateDTO foodUpdateDTO) {
        ThrowUtils.throwIf(foodUpdateDTO == null, ErrorCode.PARAMS_ERROR);
        Long foodId = foodUpdateDTO.getId();
        ThrowUtils.throwIf(foodId == null, ErrorCode.PARAMS_ERROR, "食物 ID 不能为空");

        FoodLibrary foodLibrary = foodLibraryMapper.selectById(foodId);
        ThrowUtils.throwIf(foodLibrary == null, ErrorCode.PARAMS_ERROR, "要修改的食物信息不存在");

        Long categoryId = foodUpdateDTO.getCategoryId();
        if (categoryId != null) {
            FoodCategories foodCategories = foodCategoriesMapper.selectById(categoryId);
            ThrowUtils.throwIf(foodCategories == null, ErrorCode.PARAMS_ERROR, "分类 ID 对应的分类不存在");
            foodLibrary.setCategoryName(foodCategories.getName());
        }

        // 拷贝非空字段
        BeanCopyUtils.copy(foodUpdateDTO, foodLibrary);

        int updateResult = foodLibraryMapper.updateById(foodLibrary);
        ThrowUtils.throwIf(updateResult == 0, ErrorCode.OPERATION_ERROR, "食物信息更新失败");

        // ⚠️ 重新查数据库中更新后的完整记录
        FoodLibrary updated = foodLibraryMapper.selectById(foodId);

        return BeanCopyUtils.copy(updated, FoodLibraryVO.class);
    }



    /**
     * 删除食物
     * @param foodId 要删除的食物 ID（字符串类型）
     */
    @Override
    public void deleteFood(String foodId) {
        // 参数非空校验
        ThrowUtils.throwIf(foodId == null || foodId.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "食物 ID 不能为空");

        Long id;
        try {
            id = Long.parseLong(foodId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "食物 ID 格式不正确");
        }

        // 检查是否存在该食物
        FoodLibrary foodLibrary = foodLibraryMapper.selectById(id);
        ThrowUtils.throwIf(foodLibrary == null, ErrorCode.FOOD_NOT_FOUND);

        // 执行删除
        int deleteCount = foodLibraryMapper.deleteById(id);
        ThrowUtils.throwIf(deleteCount == 0, ErrorCode.OPERATION_ERROR, "删除失败");
    }

    /**
     * 根据分类 ID 获取该分类下的食物列表
     * @param categoryId 食物分类 ID
     * @return List<FoodLibraryVO>
     */
    @Override
    public List<FoodLibraryVO> getFoodsByCategoryId(Long categoryId) {
        // 校验参数
        ThrowUtils.throwIf(categoryId == null, ErrorCode.PARAMS_ERROR, "分类 ID 不能为空");

        // 查询
        QueryWrapper<FoodLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category_id", categoryId);

        List<FoodLibrary> foodList = foodLibraryMapper.selectList(queryWrapper);

        return BeanCopyUtils.copyList(foodList, FoodLibraryVO.class);
    }

    /**
     * 根据食物 ID 获取完整的食物信息
     * @param foodId 食物 ID
     * @return FoodLibraryVO
     */
    @Override
    public FoodLibraryVO getFoodDetailById(Long foodId) {
        ThrowUtils.throwIf(foodId == null, ErrorCode.PARAMS_ERROR, "食物 ID 不能为空");

        FoodLibrary foodLibrary = foodLibraryMapper.selectById(foodId);
        ThrowUtils.throwIf(foodLibrary == null, ErrorCode.FOOD_NOT_FOUND);
        return BeanCopyUtils.copy(foodLibrary, FoodLibraryVO.class);
    }

    /**
     * 获取常见食物信息列表（不分页）
     *
     * @param categoryId 分类ID，可选
     * @return 常见食物信息列表
     */
    @Override
    public List<FoodLibraryVO> getCommonFoods(Long categoryId) {
        // 构建查询条件
        QueryWrapper<FoodLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_common", 1); // 只查询常见食物
        queryWrapper.eq("is_delete", 0); // 只查询未删除的食物

        // 如果指定了分类ID，则添加分类条件
        if (categoryId != null) {
            queryWrapper.eq("category_id", categoryId);
        }

        // 限制返回数量为20条
        Page<FoodLibrary> page = new Page<>(1, 20); // 第一页，每页20条
        IPage<FoodLibrary> foodPage = foodLibraryMapper.selectPage(page, queryWrapper);

        // 转换为VO对象列表
        return foodPage.getRecords().stream()
                .map(foodLibrary -> {
                    FoodLibraryVO vo = new FoodLibraryVO();
                    BeanUtils.copyProperties(foodLibrary, vo);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据食物名称模糊搜索（不分页，返回所有匹配结果）
     *
     * @param keyword 食物名称关键词（前端只需传这个参数）
     * @return 匹配的食物列表
     */
    @Override
    public List<FoodLibraryVO> searchFoodByName(String keyword) {
        // 构建查询条件：模糊匹配名称 + 未删除
        QueryWrapper<FoodLibrary> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("name", keyword)  // 模糊搜索
                .eq("is_delete", 0)      // 过滤已删除的食物
                .orderByAsc("name");     // 按名称升序排序（可选）

        // 查询所有匹配结果
        List<FoodLibrary> foodList = this.list(queryWrapper);

        // 转换为VO返回
        return BeanCopyUtils.copyList(foodList, FoodLibraryVO.class);
    }

    /**
     * 分页查询食物信息（支持多条件筛选）
     *
     * @param queryDTO 分页及查询条件参数
     * @return 分页结果（包含食物列表及分页信息）
     */
    @Override
    public IPage<FoodLibraryVO> pageQueryFoods(FoodPageQueryDTO queryDTO) {
        // 1. 创建分页对象
        Page<FoodLibrary> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 构建查询条件（关联食物表和分类表，筛选未删除的食物）
        LambdaQueryWrapper<FoodLibrary> queryWrapper = new LambdaQueryWrapper<>();

        // 食物ID精确查询
        if (queryDTO.getFoodId() != null) {
            queryWrapper.eq(FoodLibrary::getId, queryDTO.getFoodId());
        }

        // 食物名称模糊查询
        String foodName = queryDTO.getFoodName();
        if (foodName != null && !foodName.trim().isEmpty()) {
            queryWrapper.like(FoodLibrary::getName, foodName.trim());
        }

        // 分类名称模糊查询
        String categoryName = queryDTO.getCategoryName();
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            queryWrapper.like(FoodLibrary::getCategoryName, categoryName.trim());
        }

        // 新增：是否常见食物筛选（精确匹配 0 或 1）
        if (queryDTO.getIsCommon() != null) {
            // 确保传入的是有效值（0或1），避免无效参数
            if (queryDTO.getIsCommon() == 0 || queryDTO.getIsCommon() == 1) {
                queryWrapper.eq(FoodLibrary::getIsCommon, queryDTO.getIsCommon());
            } else {
                // 可选：如果传入无效值，可抛出异常或忽略该条件
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "isCommon 必须为 0 或 1");
            }
        }

        // 筛选未删除的食物
        queryWrapper.eq(FoodLibrary::getIsDelete, 0);
        // 排序：按创建时间降序（最新添加的食物在前）
        queryWrapper.orderByDesc(FoodLibrary::getCreateTime);

        // 3. 执行分页查询（查询食物表）
        Page<FoodLibrary> foodPage = foodLibraryMapper.selectPage(page, queryWrapper);

        // 4. 转换为VO（如果需要对结果加工，如单位转换、字段拼接等）
        List<FoodLibraryVO> foodVOList = foodPage.getRecords().stream()
                .map(foodLibrary -> {
                    FoodLibraryVO vo = new FoodLibraryVO();
                    // 字段映射（可使用BeanUtils.copyProperties或手动映射）
                    BeanUtils.copyProperties(foodLibrary, vo);
                    // 如需额外处理，例如：vo.setCaloriesDesc(foodLibrary.getCalories() + " kcal/100g");
                    return vo;
                })
                .collect(Collectors.toList());

        // 5. 封装分页结果
        IPage<FoodLibraryVO> resultPage = new Page<>();
        resultPage.setRecords(foodVOList);
        resultPage.setTotal(foodPage.getTotal()); // 总条数
        resultPage.setCurrent(foodPage.getCurrent()); // 当前页码
        resultPage.setSize(foodPage.getSize()); // 每页条数
        resultPage.setPages(foodPage.getPages()); // 总页数

        return resultPage;
    }

}