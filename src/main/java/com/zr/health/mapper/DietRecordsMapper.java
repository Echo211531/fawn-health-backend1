package com.zr.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zr.health.model.entity.DietRecords;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
* @author 27105
* @description 针对表【diet_records(饮食记录主表)】的数据库操作Mapper
* @createDate 2025-05-25 18:10:41
* @Entity com.ljh.domain.DietRecords
*/
public interface DietRecordsMapper extends BaseMapper<DietRecords> {
    List<DietRecords> selectByUserIdAndDate(@Param("userId") Long userId, @Param("recordDate") Date recordDate);

    DietRecords selectOneByUserIdMealTypeAndDate(@Param("userId") Long userId,
                                                 @Param("mealType") Integer mealType,
                                                 @Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate);

    @Select("SELECT * FROM diet_records " +
            "WHERE user_id = #{userId} " +
            "AND record_date BETWEEN #{startDate} AND #{endDate} " +
            "AND is_delete = 0 " +
            "ORDER BY record_date, meal_type")
    List<DietRecords> selectByUserIdAndDateRange(@Param("userId") Long userId,
                                                 @Param("startDate") Date startDate,
                                                 @Param("endDate") Date endDate);

}




