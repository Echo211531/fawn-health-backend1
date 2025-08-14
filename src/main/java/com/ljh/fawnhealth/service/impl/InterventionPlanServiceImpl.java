package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljh.fawnhealth.mapper.InterventionPlanMapper;
import com.ljh.fawnhealth.model.dto.rule.InterventionPlanPageQueryDTO;
import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;
import com.ljh.fawnhealth.service.InterventionPlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterventionPlanServiceImpl implements InterventionPlanService {

    private final InterventionPlanMapper mapper;

    public InterventionPlanServiceImpl(InterventionPlanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<InterventionPlan> findEnabledByRiskType(String riskType) {
        return mapper.selectEnabledByRiskType(riskType);
    }

    @Override
    public List<InterventionPlan> findAllEnabled() {
        return mapper.selectAllEnabled();
    }

    @Override
    public InterventionPlan create(InterventionPlan plan) {
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        mapper.insert(plan);
        return plan;
    }

    @Override
    public InterventionPlan update(InterventionPlan plan) {
        plan.setUpdateTime(LocalDateTime.now());
        mapper.updateById(plan);
        return plan;
    }

    @Override
    public boolean delete(String id) {
        return mapper.deleteById(id) > 0;
    }

    @Override
    public List<InterventionPlan> list(String riskType, Boolean enabled) {
        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        if (riskType != null && !riskType.isEmpty()) {
            wrapper.eq(InterventionPlan::getRiskType, riskType);
        }
        if (enabled != null) {
            wrapper.eq(InterventionPlan::isEnabled, enabled);
        }
        wrapper.orderByDesc(InterventionPlan::getCreateTime);
        return mapper.selectList(wrapper);
    }

    @Override
    public boolean toggle(String id, boolean enabled) {
        InterventionPlan plan = new InterventionPlan();
        plan.setId(id);
        plan.setEnabled(enabled);
        plan.setUpdateTime(LocalDateTime.now());
        return mapper.updateById(plan) > 0;
    }

    @Override
    public InterventionPlan getById(String id) {
        return mapper.selectById(id);
    }

    @Override
    public IPage<InterventionPlan> pageQuery(InterventionPlanPageQueryDTO queryDTO) {
        Page<InterventionPlan> page = new Page<>(
                queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum(),
                queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize());
        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO.getRiskType() != null && !queryDTO.getRiskType().isEmpty()) {
            wrapper.eq(InterventionPlan::getRiskType, queryDTO.getRiskType());
        }
        if (queryDTO.getEnabled() != null) {
            wrapper.eq(InterventionPlan::isEnabled, queryDTO.getEnabled());
        }
        wrapper.orderByDesc(InterventionPlan::getCreateTime);
        return mapper.selectPage(page, wrapper);
    }
}