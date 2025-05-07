package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.mapper.FoodCategoriesMapper;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodCategoryUpdateDTO;
import com.ljh.fawnhealth.model.entity.FoodCategories;
import com.ljh.fawnhealth.model.vo.food.FoodCategoryVO;
import com.ljh.fawnhealth.service.FoodCategoriesService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import java.util.List;

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
                        .orderByAsc(FoodCategories::getSortOrder)
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
     * 删除食物分类
     * @param categoryId 分类 ID
     */
    @Override
    public void deleteFoodCategory(Long categoryId) {
        ThrowUtils.throwIf(categoryId == null, ErrorCode.PARAMS_ERROR, "食物分类 ID 不能为空");
        foodCategoriesMapper.deleteById(categoryId);
    }

}




