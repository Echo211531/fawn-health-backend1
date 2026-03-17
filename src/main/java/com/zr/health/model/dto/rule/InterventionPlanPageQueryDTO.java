package com.zr.health.model.dto.rule;

import lombok.Data;

@Data
public class InterventionPlanPageQueryDTO {
    private String riskType;
    private Boolean enabled;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    /** 预警ID，可选：若传入则根据该预警的riskType筛选干预方案 */
    private Long warningId;
}