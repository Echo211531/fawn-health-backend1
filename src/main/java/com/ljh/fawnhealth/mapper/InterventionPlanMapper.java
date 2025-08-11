package com.ljh.fawnhealth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 干预方案 Mapper 接口
 */
public interface InterventionPlanMapper extends BaseMapper<InterventionPlan> {

    List<InterventionPlan> selectEnabledByRiskType(@Param("riskType") String riskType);

    List<InterventionPlan> selectAllEnabled();
}