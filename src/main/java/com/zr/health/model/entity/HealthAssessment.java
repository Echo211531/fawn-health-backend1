package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户健康评估每日记录表
 * @TableName health_assessment
 */
@TableName(value ="health_assessment")
@Data
public class HealthAssessment implements Serializable {
    /**
     * 主键ID，自增长
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的用户ID，对应user表的id
     */
    private Long userId;

    /**
     * 健康综合评分，范围0-100，分数越高表示健康状况越好
     */
    private Integer score;

    /**
     * 体重变化趋势(kg/周)，正数表示增重，负数表示减重
     */
    private BigDecimal weightTrend;

    /**
     * 每日热量平衡值(kcal)，正数表示热量盈余，负数表示热量缺口
     */
    private BigDecimal calorieBalance;

    /**
     * 营养均衡评分(0-100)，评估蛋白质、脂肪、碳水化合物的摄入平衡性
     */
    private Integer nutritionScore;

    /**
     * 系统生成的个性化饮食建议文本
     */
    private String dietAdvice;

    /**
     * 系统生成的个性化运动建议文本
     */
    private String exerciseAdvice;

    /**
     * 评估日期（仅日期部分）
     */
    private Date assessmentDate;

    /**
     * 记录创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}