package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zr.health.exception.ErrorCode;
import com.zr.health.exception.ThrowUtils;
import com.zr.health.mapper.InterventionPlanMapper;
import com.zr.health.mapper.HealthRiskWarningMapper;
import com.zr.health.model.dto.rule.InterventionPlanPageQueryDTO;
import com.zr.health.model.entity.HealthRiskWarning;
import com.zr.health.model.enums.rule.InterventionPlan;
import com.zr.health.service.InterventionPlanService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InterventionPlanServiceImpl implements InterventionPlanService {

    private final InterventionPlanMapper mapper;
    private final HealthRiskWarningMapper warningMapper;

    public InterventionPlanServiceImpl(InterventionPlanMapper mapper, HealthRiskWarningMapper warningMapper) {
        this.mapper = mapper;
        this.warningMapper = warningMapper;
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
        plan.setIsDelete(0);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        mapper.insert(plan);
        return plan;
    }

    @Override
    public InterventionPlan update(InterventionPlan plan) {
        ThrowUtils.throwIf(plan.getId() == null, ErrorCode.PARAMS_ERROR, "干预方案ID不能为空");
        plan.setUpdateTime(LocalDateTime.now());
        mapper.updateById(plan);
        return plan;
    }

    @Override
    public boolean delete(Long id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "干预方案ID不能为空");
        InterventionPlan exist = mapper.selectById(id);
        ThrowUtils.throwIf(exist == null, ErrorCode.NOT_FOUND_ERROR, "干预方案不存在");
        ThrowUtils.throwIf(exist.getIsDelete() != null && exist.getIsDelete() == 1, ErrorCode.OPERATION_ERROR,
                "该干预方案已被删除");
        InterventionPlan update = new InterventionPlan();
        update.setId(id);
        update.setIsDelete(1);
        update.setUpdateTime(LocalDateTime.now());
        return mapper.updateById(update) > 0;
    }

    @Override
    public List<InterventionPlan> list(String riskType, Boolean enabled) {
        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterventionPlan::getIsDelete, 0);
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
    public boolean toggle(Long id, boolean enabled) {
        InterventionPlan plan = new InterventionPlan();
        plan.setId(id);
        plan.setEnabled(enabled);
        plan.setUpdateTime(LocalDateTime.now());
        return mapper.updateById(plan) > 0;
    }

    @Override
    public InterventionPlan getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public IPage<InterventionPlan> pageQuery(InterventionPlanPageQueryDTO queryDTO) {
        Page<InterventionPlan> page = new Page<>(
                queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum(),
                queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize());
        LambdaQueryWrapper<InterventionPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InterventionPlan::getIsDelete, 0);
        // 优先根据warningId反查riskType
        if (queryDTO.getWarningId() != null) {
            HealthRiskWarning warn = warningMapper.selectById(queryDTO.getWarningId());
            if (warn != null && warn.getRiskType() != null && !warn.getRiskType().isEmpty()) {
                wrapper.eq(InterventionPlan::getRiskType, warn.getRiskType());
            }
        }
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