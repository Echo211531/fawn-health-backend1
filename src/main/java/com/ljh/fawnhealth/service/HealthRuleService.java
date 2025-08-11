package com.ljh.fawnhealth.service;

import com.ljh.fawnhealth.model.entity.HealthRule;

import java.util.List;

/**
 * 健康规则服务接口
 */
public interface HealthRuleService {

    /**
     * 创建健康规则
     */
    HealthRule createRule(HealthRule rule);

    /**
     * 更新健康规则
     */
    HealthRule updateRule(HealthRule rule);

    /**
     * 根据ID获取规则
     */
    HealthRule getRuleById(String id);

    /**
     * 获取所有规则
     */
    List<HealthRule> getAllRules();

    /**
     * 获取所有启用的规则
     */
    List<HealthRule> getEnabledRules();

    /**
     * 根据风险类型获取规则
     */
    List<HealthRule> getRulesByRiskType(String riskType);

    /**
     * 启用/禁用规则
     */
    HealthRule toggleRule(String id, boolean enabled);

    /**
     * 删除规则
     */
    boolean deleteRule(String id);

    /**
     * 验证规则表达式
     */
    boolean validateRuleExpression(String expression);
}