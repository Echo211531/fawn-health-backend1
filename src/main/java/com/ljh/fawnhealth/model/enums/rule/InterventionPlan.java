package com.ljh.fawnhealth.model.enums.rule;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 干预方案实体类
 * 用于存储针对不同健康风险的个性化干预措施
 */
@Document(collection = "intervention_plans")
@TableName("intervention_plans")
@Data
public class InterventionPlan {
    @Id
    private String id;
    
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
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}