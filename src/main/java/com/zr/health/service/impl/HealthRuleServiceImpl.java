package com.zr.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zr.health.mapper.HealthRuleMapper;
import com.zr.health.model.dto.healthRule.HealthRuleAddDTO;
import com.zr.health.model.entity.HealthRule;
import com.zr.health.service.HealthRuleService;
import com.zr.health.utils.BeanCopyUtils;
import com.zr.health.utils.MvelUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康规则服务实现类
 */
@Service
public class HealthRuleServiceImpl implements HealthRuleService {

    @Resource
    private HealthRuleMapper healthRuleMapper;

    @Override
    public HealthRule createRule(HealthRuleAddDTO rule) {
        HealthRule healthRule = new HealthRule();
        BeanCopyUtils.copy(rule, healthRule);
        healthRule.setCreateTime(LocalDateTime.now());
        healthRule.setUpdateTime(LocalDateTime.now());
        healthRuleMapper.insert(healthRule);
        return healthRule;
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
    public IPage<HealthRule> getRulesByCondition(String id, String name, Integer enabled, Integer pageNum, Integer pageSize) {
        // 创建分页对象
        IPage<HealthRule> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<HealthRule> wrapper = new LambdaQueryWrapper<>();

        // 动态添加条件：如果参数不为空，则加入查询条件
        if (StringUtils.hasText(id)) {
            wrapper.eq(HealthRule::getId, id);  // 精确匹配ID
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(HealthRule::getName, name);  // 模糊匹配名称
        }
        if (enabled != null) {
            wrapper.eq(HealthRule::getEnabled, enabled);  // 精确匹配状态（0或1）
        }

        // 保持原有的排序方式
        wrapper.orderByAsc(HealthRule::getPriority).orderByAsc(HealthRule::getCreateTime);

        // 执行分页查询
        return healthRuleMapper.selectPage(page, wrapper);
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