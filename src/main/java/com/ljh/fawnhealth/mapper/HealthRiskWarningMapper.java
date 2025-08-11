package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.entity.HealthRiskWarning;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康风险预警Mapper接口
 */
public interface HealthRiskWarningMapper extends BaseMapper<HealthRiskWarning> {

    /**
     * 查询用户的风险预警记录
     */
    List<HealthRiskWarning> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户指定时间范围内的风险预警记录
     */
    List<HealthRiskWarning> selectByUserIdAndTimeRange(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 查询未处理的风险预警记录
     */
    List<HealthRiskWarning> selectUnprocessedWarnings();

    /**
     * 根据风险类型查询预警记录
     */
    List<HealthRiskWarning> selectByRiskType(@Param("riskType") String riskType);
}