package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.mapper.FoodCategoriesMapper;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryPageQueryDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryUpdateDTO;
import com.ljh.fawnhealth.model.entity.FoodCategories;
import com.ljh.fawnhealth.model.vo.food.FoodCategoryVO;
import com.ljh.fawnhealth.service.FoodCategoriesService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import org.apache.tika.utils.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 食物分类服务实现类
 */
@Service
public class FoodCategoriesServiceImpl extends ServiceImpl<FoodCategoriesMapper, FoodCategories>
        implements FoodCategoriesService {

    @Resource
    private FoodCategoriesMapper foodCategoriesMapper;

    /**
     * 获取所有食物分类，按排序字段升序排列
     * @return 食物分类列表（VO）
     */
    @Override
    public List<FoodCategoryVO> getFoodCategoriesList() {
        List<FoodCategories> entityList = foodCategoriesMapper.selectList(
                new QueryWrapper<FoodCategories>()
                        .lambda()
                        // 核心：添加过滤条件
                        .eq(FoodCategories::getIsDelete, 0)  // 排除已删除的分类
                        .eq(FoodCategories::getStatus, 1)    // 排除禁用的分类（只保留启用状态）
                        .orderByAsc(FoodCategories::getSortOrder)  // 按排序权重升序
        );

        return BeanCopyUtils.copyList(entityList, FoodCategoryVO.class);
    }

    /**
     * 添加食物分类
     * @param foodCategoryAddDTO 分类信息
     * @return 添加后的分类信息
     */
    @Override
    public FoodCategoryVO addFoodCategory(FoodCategoryAddDTO foodCategoryAddDTO) {
        ThrowUtils.throwIf(foodCategoryAddDTO == null || foodCategoryAddDTO.getName() == null, ErrorCode.PARAMS_ERROR);
        // 拷贝 DTO 到实体类
        FoodCategories category = BeanCopyUtils.copy(foodCategoryAddDTO, FoodCategories.class);

        // 插入数据库
        foodCategoriesMapper.insert(category);

        // 再次从实体构建 VO（确保拿到 ID）
        return BeanCopyUtils.copy(category, FoodCategoryVO.class);
    }



    /**
     * 修改食物分类
     * @param foodCategoryUpdateDTO 分类信息
     * @return 修改后的分类信息
     */
    @Override
    public FoodCategoryVO updateFoodCategory(FoodCategoryUpdateDTO foodCategoryUpdateDTO) {
        ThrowUtils.throwIf(foodCategoryUpdateDTO == null || foodCategoryUpdateDTO.getId() == null, ErrorCode.PARAMS_ERROR);

        // 查询原有记录
        FoodCategories exist = foodCategoriesMapper.selectById(foodCategoryUpdateDTO.getId());
        ThrowUtils.throwIf(exist == null, ErrorCode.FOOD_CATEGORY_NOT_FOUND);

        // 有值才更新字段（保留原值）
        if (foodCategoryUpdateDTO.getName() != null) {
            exist.setName(foodCategoryUpdateDTO.getName());
        }
        if (foodCategoryUpdateDTO.getIcon() != null) {
            exist.setIcon(foodCategoryUpdateDTO.getIcon());
        }
        if (foodCategoryUpdateDTO.getDescription() != null) {
            exist.setDescription(foodCategoryUpdateDTO.getDescription());
        }
        if (foodCategoryUpdateDTO.getStatus() != null) {
            exist.setStatus(foodCategoryUpdateDTO.getStatus());
        }
        if (foodCategoryUpdateDTO.getSortOrder() != null) {
            exist.setSortOrder(foodCategoryUpdateDTO.getSortOrder());
        }

        // 执行更新
        foodCategoriesMapper.updateById(exist);

        // 查询数据库
        FoodCategories foodCategories = foodCategoriesMapper.selectById(foodCategoryUpdateDTO.getId());

        return BeanCopyUtils.copy(foodCategories, FoodCategoryVO.class);
    }




    /**
     * 删除食物分类（逻辑删除）
     * @param categoryId 分类 ID
     */
    @Override
    public void deleteFoodCategory(Long categoryId) {
        // 1. 校验分类ID是否为空
        ThrowUtils.throwIf(categoryId == null, ErrorCode.PARAMS_ERROR, "食物分类 ID 不能为空");

        // 2. 查询分类是否存在（且未被删除）
        FoodCategories foodCategory = foodCategoriesMapper.selectById(categoryId);
        ThrowUtils.throwIf(foodCategory == null, ErrorCode.NOT_FOUND_ERROR, "食物分类不存在");
        ThrowUtils.throwIf(foodCategory.getIsDelete() == 1, ErrorCode.OPERATION_ERROR, "该分类已被删除");

        // 3. 执行逻辑删除（更新 is_delete 为 1）
        FoodCategories updateCategory = new FoodCategories();
        updateCategory.setId(categoryId);
        updateCategory.setIsDelete(1); // 标记为已删除
        int updateCount = foodCategoriesMapper.updateById(updateCategory);

        // 4. 校验更新结果
        ThrowUtils.throwIf(updateCount <= 0, ErrorCode.OPERATION_ERROR, "删除食物分类失败");
    }

    /**
     * 分页查询食物分类列表
     *
     * @param queryDTO
     * @return
     */
    @Override
    public IPage<FoodCategoryVO> pageQueryFoodCategories(FoodCategoryPageQueryDTO queryDTO) {
        // 创建分页对象
        Page<FoodCategories> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 构建查询条件
        LambdaQueryWrapper<FoodCategories> queryWrapper = new LambdaQueryWrapper<>();

        // 分类ID精确查询
        if (queryDTO.getCategoryId() != null) {
            queryWrapper.eq(FoodCategories::getId, queryDTO.getCategoryId());
        }

        // 分类名称模糊查询
        if (queryDTO.getName() != null && !queryDTO.getName().trim().isEmpty()) {
            queryWrapper.like(FoodCategories::getName, queryDTO.getName().trim());
        }

        // 分类状态精确查询
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(FoodCategories::getStatus, queryDTO.getStatus());
        }

        // 排除已删除的分类
        queryWrapper.eq(FoodCategories::getIsDelete, 0);

        // 按排序权重和创建时间排序
        queryWrapper.orderByDesc(FoodCategories::getSortOrder)
                .orderByDesc(FoodCategories::getCreateTime);

        // 执行分页查询
        Page<FoodCategories> foodCategoriesPage = foodCategoriesMapper.selectPage(page, queryWrapper);

        // 转换为VO对象
        List<FoodCategoryVO> categoryVOList = foodCategoriesPage.getRecords().stream()
                .map(category -> {
                    FoodCategoryVO vo = new FoodCategoryVO();
                    BeanUtils.copyProperties(category, vo);
                    return vo;
                })
                .collect(Collectors.toList());

        // 构建返回的分页结果
        Page<FoodCategoryVO> resultPage = new Page<>();
        resultPage.setRecords(categoryVOList);
        resultPage.setTotal(foodCategoriesPage.getTotal());
        resultPage.setCurrent(foodCategoriesPage.getCurrent());
        resultPage.setSize(foodCategoriesPage.getSize());
        resultPage.setPages(foodCategoriesPage.getPages());

        return resultPage;
    }

}




