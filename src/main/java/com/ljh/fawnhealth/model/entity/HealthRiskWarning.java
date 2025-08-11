package com.ljh.fawnhealth.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 健康风险预警记录表
 * 
 * @TableName health_risk_warnings
 */
@TableName(value = "health_risk_warnings")
@Data
public class HealthRiskWarning implements Serializable {
    /**
     * 预警ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 触发规则ID
     */
    private String ruleId;

    /**
     * 触发时的数据快照
     */
    private String triggerData;

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}