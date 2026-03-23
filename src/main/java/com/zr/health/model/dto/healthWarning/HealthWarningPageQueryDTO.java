package com.zr.health.model.dto.healthWarning;

import lombok.Data;

/**
 * 管理端分页查询预警参数
 */
@Data
public class HealthWarningPageQueryDTO {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 状态：0未处理，1已处理
     */
    private Integer status;
}
