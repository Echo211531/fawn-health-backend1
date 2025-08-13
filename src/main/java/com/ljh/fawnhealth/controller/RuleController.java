package com.ljh.fawnhealth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.model.dto.healthRule.HealthRuleAddDTO;
import com.ljh.fawnhealth.model.entity.HealthRule;
import com.ljh.fawnhealth.service.HealthRuleService;
import com.ljh.fawnhealth.service.RuleEngineService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rules")
public class RuleController {

    private static final Logger log = LoggerFactory.getLogger(RuleController.class);
    @Resource
    private HealthRuleService healthRuleService;

    @Resource
    private RuleEngineService ruleEngineService;

    // 创建规则
    @PostMapping("/createRule")
    public BaseResponse<HealthRule> createRule(@RequestBody HealthRuleAddDTO rule) {
        HealthRule createdRule = healthRuleService.createRule(rule);
        ruleEngineService.reloadRules(); // 重新加载规则
        return ResultUtils.success(createdRule);
    }

    // 更新规则
    @PostMapping("/updateRule/{id}")
    public BaseResponse<HealthRule> updateRule(@PathVariable String id, @RequestBody HealthRule rule) {
        rule.setId(id);
        HealthRule updatedRule = healthRuleService.updateRule(rule);
        ruleEngineService.reloadRules(); // 重新加载规则
        return ResultUtils.success(updatedRule);
    }

    /**
     * 分页查询规则信息
     *
     * @param id
     * @param name
     * @param enabled
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/getAllRules")
    public BaseResponse<IPage<HealthRule>> getAllRules(
            @RequestParam(required = false) String id,  // 规则ID，非必需
            @RequestParam(required = false) String name,  // 规则名称，非必需
            @RequestParam(required = false) Integer enabled,  // 规则状态，非必需
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        log.info("调用分页查询规则信息接口");
        // 调用Service层，传入查询条件和分页参数
        return ResultUtils.success(healthRuleService.getRulesByCondition(id, name, enabled, pageNum, pageSize));
    }

    // 获取启用的规则
    @GetMapping("/enabled")
    public BaseResponse<List<HealthRule>> getEnabledRules() {
        return ResultUtils.success(healthRuleService.getEnabledRules());
    }

    // 根据风险类型获取规则
    @GetMapping("/riskType/{riskType}")
    public BaseResponse<List<HealthRule>> getRulesByRiskType(@PathVariable String riskType) {
        return ResultUtils.success(healthRuleService.getRulesByRiskType(riskType));
    }

    // 启用/禁用规则
    @PostMapping("/{id}/toggle")
    public BaseResponse<HealthRule> toggleRule(@PathVariable String id, @RequestParam boolean enabled) {
        HealthRule rule = healthRuleService.toggleRule(id, enabled);
        ruleEngineService.reloadRules();
        return ResultUtils.success(rule);
    }

    // 删除规则
    @PostMapping("/deleteRule/{id}")
    public BaseResponse<String> deleteRule(@PathVariable String id) {
        boolean ok = healthRuleService.deleteRule(id);
        if (ok) {
            ruleEngineService.reloadRules();
        }
        return ResultUtils.success("删除成功");
    }

    // 验证规则表达式
    @PostMapping("/validateExpression")
    public boolean validateExpression(@RequestBody String expression) {
        return healthRuleService.validateRuleExpression(expression);
    }
}