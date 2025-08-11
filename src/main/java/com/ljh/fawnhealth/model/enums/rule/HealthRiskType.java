package com.ljh.fawnhealth.model.enums.rule;

/**
 * 健康风险类型枚举
 */
public enum HealthRiskType {

    DIABETES_RISK("糖尿病风险", "连续高糖摄入可能导致糖尿病风险"),
    HYPERTENSION_RISK("高血压风险", "连续高盐摄入可能导致高血压风险"),
    OBESITY_RISK("肥胖风险", "连续高脂肪摄入可能导致肥胖风险"),
    CARDIOVASCULAR_RISK("心血管疾病风险", "连续高胆固醇摄入可能导致心血管疾病风险"),
    NUTRITION_DEFICIENCY("营养不足", "连续营养摄入不足可能导致营养不良");

    private final String name;
    private final String description;

    HealthRiskType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}