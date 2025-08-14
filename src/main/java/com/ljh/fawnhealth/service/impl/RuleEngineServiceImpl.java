package com.ljh.fawnhealth.service.impl;

import com.ljh.fawnhealth.model.entity.HealthRule;
import com.ljh.fawnhealth.model.vo.food.DietRecordSimpleVO;
import com.ljh.fawnhealth.model.vo.rule.HealthRiskWarningVO;
import com.ljh.fawnhealth.model.entity.HealthRiskWarning;
import com.ljh.fawnhealth.model.enums.rule.InterventionPlan;
import com.ljh.fawnhealth.service.DietRecordsService;
import com.ljh.fawnhealth.service.HealthRuleService;
import com.ljh.fawnhealth.service.HealthRiskWarningService;
import com.ljh.fawnhealth.service.InterventionPlanService;
import com.ljh.fawnhealth.service.RuleEngineService;
import com.ljh.fawnhealth.utils.MvelUtil;
import com.ljh.fawnhealth.utils.NutritionCalculatorUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.jeasy.rules.core.RuleBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则引擎服务实现类
 * 基于EasyRules实现健康风险评估
 */
@Service
@Slf4j
public class RuleEngineServiceImpl implements RuleEngineService {

    @Autowired
    private DietRecordsService dietRecordsService;

    @Autowired
    private HealthRuleService healthRuleService;

    @Autowired
    private InterventionPlanService interventionPlanService;

    @Autowired
    private HealthRiskWarningService healthRiskWarningService;

    // 规则缓存
    private List<Rule> rules = new ArrayList<>();
    private Map<String, String> ruleNameByRiskType = new HashMap<>();
    private Map<String, Long> ruleIdByRiskType = new HashMap<>();

    @PostConstruct
    public void init() {
        loadRules();
    }

    @Override
    public void reloadRules() {
        loadRules();
    }

    private void loadRules() {
        try {
            List<HealthRule> dbRules = healthRuleService.getEnabledRules();
            rules = dbRules.stream()
                    .map(this::convertToEasyRule)
                    .collect(Collectors.toList());
            ruleNameByRiskType = dbRules.stream()
                    .collect(Collectors.toMap(HealthRule::getRiskType, HealthRule::getName, (a, b) -> a));
            ruleIdByRiskType = dbRules.stream()
                    .collect(Collectors.toMap(HealthRule::getRiskType, HealthRule::getId, (a, b) -> a));
            log.info("成功加载 {} 条健康规则", rules.size());
        } catch (Exception e) {
            log.error("加载健康规则失败", e);
            rules = new ArrayList<>();
            ruleNameByRiskType = new HashMap<>();
            ruleIdByRiskType = new HashMap<>();
        }
    }

    private Rule convertToEasyRule(HealthRule healthRule) {
        return new RuleBuilder()
                .name(healthRule.getName())
                .description(healthRule.getDescription())
                .when(facts -> {
                    try {
                        return MvelUtil.executeBoolean(healthRule.getConditionExpr(), facts.asMap());
                    } catch (Exception e) {
                        log.error("规则执行错误: {}", healthRule.getId(), e);
                        return false;
                    }
                })
                .then(facts -> {
                    @SuppressWarnings("unchecked")
                    Set<String> riskTypes = (Set<String>) facts.get("riskTypes");
                    if (riskTypes != null) {
                        riskTypes.add(healthRule.getRiskType());
                    }
                })
                .build();
    }

    @Override
    public List<HealthRiskWarningVO> evaluateUserHealthRisk(String userId) {
        try {
            List<DietRecordSimpleVO> records = getLast7DaysRecords(userId);

            Facts facts = prepareFacts(records);
            Set<String> riskTypes = new HashSet<>();
            facts.put("riskTypes", riskTypes);

            RulesEngine rulesEngine = new DefaultRulesEngine();
            Rules rulesCollection = new Rules();
            rules.forEach(rulesCollection::register);
            rulesEngine.fire(rulesCollection, facts);

            List<HealthRiskWarningVO> result = riskTypes.stream()
                    .map(riskType -> createWarning(userId, riskType, facts))
                    .collect(Collectors.toList());

            // 落库
            for (HealthRiskWarningVO vo : result) {
                HealthRiskWarning entity = new HealthRiskWarning();
                entity.setUserId(vo.getUserId());
                entity.setRiskType(vo.getRiskType());
                entity.setRuleId(ruleIdByRiskType.get(vo.getRiskType()));
                entity.setTriggerData(toJsonSafe(vo.getTriggerData()));
                entity.setInterventionContent(vo.getInterventionContent());
                entity.setStatus(0);
                entity.setTriggerTime(vo.getTriggerTime());
                entity.setCreateTime(LocalDateTime.now());
                healthRiskWarningService.save(entity);
            }

            return result;

        } catch (Exception e) {
            log.error("评估用户健康风险失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private List<DietRecordSimpleVO> getLast7DaysRecords(String userId) {
        try {
            return dietRecordsService.getHistoryRecords(Long.valueOf(userId), 7);
        } catch (Exception e) {
            log.error("获取用户饮食记录失败，用户ID: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private Facts prepareFacts(List<DietRecordSimpleVO> records) {
        Facts facts = new Facts();
        // 7天平均营养
        Map<String, java.math.BigDecimal> avg = NutritionCalculatorUtil.calculate7DayAverage(records);
        facts.put("avgCalories", avg.getOrDefault("avgCalories", java.math.BigDecimal.ZERO));
        facts.put("avgProtein", avg.getOrDefault("avgProtein", java.math.BigDecimal.ZERO));
        facts.put("avgFat", avg.getOrDefault("avgFat", java.math.BigDecimal.ZERO));
        facts.put("avgCarbohydrate", avg.getOrDefault("avgCarbohydrate", java.math.BigDecimal.ZERO));

        // 连续天数指标
        Map<String, Integer> consecutiveDays = NutritionCalculatorUtil.calculateConsecutiveDays(records);
        facts.put("consecutiveLowVegetableDays", consecutiveDays.getOrDefault("lowVegetable", 0));
        facts.put("consecutiveHighFatDays", consecutiveDays.getOrDefault("highFat", 0));
        facts.put("consecutiveHighCarbDays", consecutiveDays.getOrDefault("highCarb", 0));

        // 记录数
        facts.put("recordCount", records == null ? 0 : records.size());
        return facts;
    }

    private String toJsonSafe(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private HealthRiskWarningVO createWarning(String userId, String riskType, Facts facts) {
        HealthRiskWarningVO warning = new HealthRiskWarningVO();
        warning.setUserId(Long.valueOf(userId));
        warning.setRiskType(riskType);
        warning.setRuleId(String.valueOf(ruleIdByRiskType.get(riskType)));
        warning.setRuleName(ruleNameByRiskType.get(riskType));
        warning.setTriggerTime(LocalDateTime.now());
        warning.setTriggerData(facts.asMap());
        warning.setStatus(0);
        warning.setInterventionContent(getInterventionContent(riskType));
        return warning;
    }

    private String getInterventionContent(String riskType) {
        try {
            List<InterventionPlan> plans = interventionPlanService.findEnabledByRiskType(riskType);
            if (plans != null && !plans.isEmpty()) {
                return plans.get(0).getContent();
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    @Override
    public List<HealthRule> getAllEnabledRules() {
        return healthRuleService.getEnabledRules();
    }

    @Override
    public List<HealthRule> getRulesByRiskType(String riskType) {
        return healthRuleService.getRulesByRiskType(riskType);
    }

    @Override
    public boolean validateRuleExpression(String expression) {
        return MvelUtil.isValidExpression(expression);
    }

    @Override
    public boolean testRuleExecution(String ruleId, Map<String, Object> testData) {
        try {
            HealthRule rule = healthRuleService.getRuleById(ruleId);
            if (rule == null || !Boolean.TRUE.equals(rule.getEnabled())) {
                return false;
            }
            return MvelUtil.executeBoolean(rule.getConditionExpr(), testData);
        } catch (Exception e) {
            log.error("测试规则执行失败，规则ID: {}", ruleId, e);
            return false;
        }
    }
}