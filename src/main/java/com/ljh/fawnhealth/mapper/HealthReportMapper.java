package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.HealthReport;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;


/**
* @author 27105
* @description 针对表【health_report(用户健康分析报告表)】的数据库操作Mapper
* @createDate 2025-08-09 20:17:54
* @Entity com.ljh.domain.HealthReport
*/
public interface HealthReportMapper extends BaseMapper<HealthReport> {

    @Select("SELECT * FROM health_report WHERE user_id = #{userId} " +
            "AND report_type = #{reportType} " +
            "ORDER BY end_date DESC LIMIT #{limit}")
    List<HealthReport> selectByUserAndType(@Param("userId") Long userId,
                                           @Param("reportType") Integer reportType,
                                           @Param("limit") Integer limit);

}




