package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 健康建议规则表
 * @TableName advice_rules
 */
@TableName(value ="advice_rules")
@Data
public class AdviceRules implements Serializable {
    /**
     * 规则ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 条件表达式，如: score<60 AND weight_trend>0.5
     */
    private String conditionExpr;

    /**
     * 建议类型：1-饮食，2-运动
     */
    private Integer adviceType;

    /**
     * 建议内容文本
     */
    private String adviceText;

    /**
     * 优先级(1-10)，数值越小优先级越高
     */
    private Integer priority;

    /**
     * 创建时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}