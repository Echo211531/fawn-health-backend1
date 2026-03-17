package com.zr.health.service;

import com.zr.health.model.vo.rule.HealthRiskWarningVO;
import com.zr.health.model.entity.HealthRule;

import java.util.List;
import java.util.Map;

/**
 * 规则引擎服务接口
 * 提供健康风险评估和规则管理功能
 */
public interface RuleEngineService {

    /**
     * 执行用户健康风险评估
     * 
     * @param userId 用户ID
     * @return 风险预警结果列表
     */
    List<HealthRiskWarningVO> evaluateUserHealthRisk(String userId);

    /**
     * 重新加载规则（管理员修改规则后调用）
     */
    void reloadRules();

    /**
     * 获取所有启用的规则
     */
    List<HealthRule> getAllEnabledRules();

    /**
     * 根据风险类型获取规则
     */
    List<HealthRule> getRulesByRiskType(String riskType);

    /**
     * 验证规则表达式
     */
    boolean validateRuleExpression(String expression);

    /**
     * 测试规则执行
     */
    boolean testRuleExecution(String ruleId, Map<String, Object> testData);
}