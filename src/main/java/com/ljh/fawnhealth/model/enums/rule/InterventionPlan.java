package com.ljh.fawnhealth.model.enums.rule;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 干预方案实体类
 * 改为使用 MySQL 存储
 */
@TableName("intervention_plans")
@Data
public class InterventionPlan {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 风险类型
     */
    private String riskType;

    /**
     * 干预方案标题
     */
    private String title;

    /**
     * 干预方案内容
     */
    private String content;

    /**
     * 干预方案类型：DIET-饮食调整，EXERCISE-运动建议，LIFESTYLE-生活方式
     */
    private String interventionType;

    /**
     * 适用人群
     */
    private String targetAudience;

    /**
     * 预期效果
     */
    private String expectedOutcome;

    /**
     * 注意事项
     */
    private String precautions;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 逻辑删除：0未删除 1已删除
     */
    private Integer isDelete;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}