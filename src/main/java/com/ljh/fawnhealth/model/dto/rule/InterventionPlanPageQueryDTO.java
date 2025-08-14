package com.ljh.fawnhealth.model.dto.rule;

import lombok.Data;

@Data
public class InterventionPlanPageQueryDTO {
    private String riskType;
    private Boolean enabled;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}