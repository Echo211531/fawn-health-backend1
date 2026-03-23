package com.zr.health.model.dto.healthWarning;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端新增预警请求参数
 */
@Data
public class HealthWarningAddDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 触发数据快照
     */
    private String triggerData;

    /**
     * 干预内容
     */
    private String interventionContent;

    /**
     * 处理状态：0未处理，1已处理
     */
    private Integer status;

    /**
     * 触发时间
     */
    private LocalDateTime triggerTime;
}
