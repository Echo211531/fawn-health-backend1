package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.HealthAssessment;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;


/**
* @author 27105
* @description 针对表【health_assessment(用户健康评估每日记录表)】的数据库操作Mapper
* @createDate 2025-08-09 20:15:30
* @Entity com.ljh.domain.HealthAssessment
*/
public interface HealthAssessmentMapper extends BaseMapper<HealthAssessment> {

    @Select("SELECT * FROM health_assessment WHERE user_id = #{userId} " +
            "AND assessment_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY assessment_date DESC")
    List<HealthAssessment> selectByDateRange(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM health_assessment WHERE user_id = #{userId} " +
            "ORDER BY assessment_date DESC LIMIT 1")
    HealthAssessment selectLatestByUser(@Param("userId") Long userId);

    boolean existsWithLock(Long userId, Date assessmentDate);
}




