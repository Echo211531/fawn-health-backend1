package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.DietFoodItems;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 针对表【diet_food_items(饮食记录-食物项表)】的数据库操作Mapper
 */
public interface DietFoodItemsMapper extends BaseMapper<DietFoodItems> {

    /**
     * 根据饮食记录ID查询对应的食物项列表
     */
    List<DietFoodItems> selectByRecordId(@Param("recordId") Long recordId);

    /**
     * 根据饮食记录ID逻辑删除对应的食物项
     */
    @Update("UPDATE diet_food_items SET is_delete=1, update_time=NOW() WHERE record_id = #{recordId}")
    int softDeleteByRecordId(@Param("recordId") Long recordId);

    int deleteByRecordId(@Param("recordId") Long recordId);

    int batchInsert(@Param("list") List<DietFoodItems> list);

    List<DietFoodItems> selectByRecordIds(List<Long> recordIds);
}
