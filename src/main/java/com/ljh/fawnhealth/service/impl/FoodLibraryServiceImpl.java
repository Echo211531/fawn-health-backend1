package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ljh.fawnhealth.exception.BusinessException;
import com.ljh.fawnhealth.exception.ErrorCode;
import com.ljh.fawnhealth.exception.ThrowUtils;
import com.ljh.fawnhealth.mapper.FoodCategoriesMapper;
import com.ljh.fawnhealth.mapper.FoodLibraryMapper;
import com.ljh.fawnhealth.model.dto.food.FoodAddDTO;
import com.ljh.fawnhealth.model.dto.food.FoodUpdateDTO;
import com.ljh.fawnhealth.model.entity.FoodCategories;
import com.ljh.fawnhealth.model.entity.FoodLibrary;
import com.ljh.fawnhealth.model.vo.food.FoodLibraryVO;
import com.ljh.fawnhealth.service.FoodLibraryService;
import com.ljh.fawnhealth.utils.BeanCopyUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.List;

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

}