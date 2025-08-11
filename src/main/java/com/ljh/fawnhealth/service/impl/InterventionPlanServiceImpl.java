package com.ljh.fawnhealth.service.impl;

import com.ljh.fawnhealth.mapper.InterventionPlanMapper;
import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;
import com.ljh.fawnhealth.service.InterventionPlanService;
import org.springframework.stereotype.Service;

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
}