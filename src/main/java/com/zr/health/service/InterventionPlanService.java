package com.zr.health.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zr.health.model.dto.rule.InterventionPlanPageQueryDTO;
import com.zr.health.model.enums.rule.InterventionPlan;

import java.util.List;

public interface InterventionPlanService {

    List<InterventionPlan> findEnabledByRiskType(String riskType);

    List<InterventionPlan> findAllEnabled();

    InterventionPlan create(InterventionPlan plan);

    InterventionPlan update(InterventionPlan plan);

    boolean delete(Long id);

    /**
     * 按条件查询
     * 
     * @param riskType 可选
     * @param enabled  可选
     */
    List<InterventionPlan> list(String riskType, Boolean enabled);

    /**
     * 启用/禁用
     */
    boolean toggle(Long id, boolean enabled);

    /**
     * 根据ID获取详情
     */
    InterventionPlan getById(Long id);

    /**
     * 分页查询
     */
    IPage<InterventionPlan> pageQuery(InterventionPlanPageQueryDTO queryDTO);
}