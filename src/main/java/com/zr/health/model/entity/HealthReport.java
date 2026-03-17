package com.zr.health.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户健康分析报告表
 * @TableName health_report
 */
@TableName(value ="health_report")
@Data
public class HealthReport implements Serializable {
    /**
     * 主键ID，自增长
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的用户ID，对应user表的id
     */
    private Long userId;

    /**
     * 报告类型：1-周报，2-月报
     */
    private Integer reportType;

    /**
     * 报告内容JSON格式，包含图表数据、趋势分析等结构化数据
     */
    private Object content;

    /**
     * 报告统计周期开始日期
     */
    private Date startDate;

    /**
     * 报告统计周期结束日期
     */
    private Date endDate;

    /**
     * 报告生成时间
     */
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}