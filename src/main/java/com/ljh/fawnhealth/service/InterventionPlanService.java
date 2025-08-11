package com.ljh.fawnhealth.service;

import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;

import java.util.List;

public interface InterventionPlanService {

    List<InterventionPlan> findEnabledByRiskType(String riskType);

    List<InterventionPlan> findAllEnabled();
}