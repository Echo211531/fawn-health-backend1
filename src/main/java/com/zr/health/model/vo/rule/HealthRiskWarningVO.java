package com.zr.health.model.vo.rule;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 健康风险预警VO
 */
@Data
public class HealthRiskWarningVO {

    /**
     * 预警ID
     */
    private String id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 风险类型名称
     */
    private String riskTypeName;

    /**
     * 触发规则ID
     */
    private String ruleId;

    /**
     * 触发规则名称
     */
    private String ruleName;

    /**
     * 触发时的数据快照
     */
    private Map<String, Object> triggerData;

    /**
     * 干预方案内容
     */
    private String interventionContent;

    /**
     * 状态：0-未处理，1-已处理
     */
    private Integer status;

    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;
}