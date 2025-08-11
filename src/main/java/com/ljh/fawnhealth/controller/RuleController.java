package com.ljh.fawnhealth.controller;

import com.ljh.fawnhealth.model.entity.HealthRule;
import com.ljh.fawnhealth.service.HealthRuleService;
import com.ljh.fawnhealth.service.RuleEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {

    @Autowired
    private HealthRuleService healthRuleService;

    @Autowired
    private RuleEngineService ruleEngineService;

    // 创建规则
    @PostMapping
    public HealthRule createRule(@RequestBody HealthRule rule) {
        HealthRule createdRule = healthRuleService.createRule(rule);
        ruleEngineService.reloadRules(); // 重新加载规则
        return createdRule;
    }

    // 更新规则
    @PutMapping("/{id}")
    public HealthRule updateRule(@PathVariable String id, @RequestBody HealthRule rule) {
        rule.setId(id);
        HealthRule updatedRule = healthRuleService.updateRule(rule);
        ruleEngineService.reloadRules(); // 重新加载规则
        return updatedRule;
    }

    // 获取所有规则
    @GetMapping
    public List<HealthRule> getAllRules() {
        return healthRuleService.getAllRules();
    }

    // 获取启用的规则
    @GetMapping("/enabled")
    public List<HealthRule> getEnabledRules() {
        return healthRuleService.getEnabledRules();
    }

    // 根据风险类型获取规则
    @GetMapping("/risk-type/{riskType}")
    public List<HealthRule> getRulesByRiskType(@PathVariable String riskType) {
        return healthRuleService.getRulesByRiskType(riskType);
    }

    // 启用/禁用规则
    @PatchMapping("/{id}/toggle")
    public HealthRule toggleRule(@PathVariable String id, @RequestParam boolean enabled) {
        HealthRule rule = healthRuleService.toggleRule(id, enabled);
        ruleEngineService.reloadRules();
        return rule;
    }

    // 删除规则
    @DeleteMapping("/{id}")
    public boolean deleteRule(@PathVariable String id) {
        boolean ok = healthRuleService.deleteRule(id);
        if (ok) {
            ruleEngineService.reloadRules();
        }
        return ok;
    }

    // 验证规则表达式
    @PostMapping("/validate-expression")
    public boolean validateExpression(@RequestBody String expression) {
        return healthRuleService.validateRuleExpression(expression);
    }
}