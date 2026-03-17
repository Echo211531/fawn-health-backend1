package com.zr.health.model.dto.healthRule;

import lombok.Data;

@Data
public class HealthRuleAddDTO {
    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 触发条件表达式
     */
    private String conditionExpr;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用
     */
    private Boolean enabled;
}
