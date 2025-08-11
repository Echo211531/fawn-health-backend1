package com.ljh.fawnhealth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ljh.fawnhealth.mapper.HealthRuleMapper;
import com.ljh.fawnhealth.model.entity.HealthRule;
import com.ljh.fawnhealth.service.HealthRuleService;
import com.ljh.fawnhealth.utils.MvelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康规则服务实现类
 */
@Service
public class HealthRuleServiceImpl implements HealthRuleService {

    @Autowired
    private HealthRuleMapper healthRuleMapper;

    @Override
    public HealthRule createRule(HealthRule rule) {
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        healthRuleMapper.insert(rule);
        return rule;
    }

    @Override
    public HealthRule updateRule(HealthRule rule) {
        rule.setUpdateTime(LocalDateTime.now());
        healthRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    public HealthRule getRuleById(String id) {
        return healthRuleMapper.selectById(id);
    }

    @Override
    public List<HealthRule> getAllRules() {
        LambdaQueryWrapper<HealthRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(HealthRule::getPriority).orderByAsc(HealthRule::getCreateTime);
        return healthRuleMapper.selectList(wrapper);
    }

    @Override
    public List<HealthRule> getEnabledRules() {
        return healthRuleMapper.selectEnabledRulesOrderByPriority();
    }

    @Override
    public List<HealthRule> getRulesByRiskType(String riskType) {
        return healthRuleMapper.selectByRiskTypeAndEnabled(riskType);
    }

    @Override
    public HealthRule toggleRule(String id, boolean enabled) {
        HealthRule rule = healthRuleMapper.selectById(id);
        if (rule != null) {
            rule.setEnabled(enabled);
            rule.setUpdateTime(LocalDateTime.now());
            healthRuleMapper.updateById(rule);
        }
        return rule;
    }

    @Override
    public boolean deleteRule(String id) {
        return healthRuleMapper.deleteById(id) > 0;
    }

    @Override
    public boolean validateRuleExpression(String expression) {
        return MvelUtil.isValidBooleanExpression(expression);
    }
}