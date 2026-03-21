package com.zr.health.model.dto.ai;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 饮食计划生成请求参数。
 * <p>根据当前体重、目标体重、计划天数与每日热量预算，由大模型生成可参考的一日饮食建议。</p>
 */
@Data
public class DietPlanRequestDTO {

    /**
     * 当前体重（kg），可选；未传时模型按「未知」处理
     */
    private BigDecimal currentWeight;

    /**
     * 目标体重（kg），必填
     */
    private BigDecimal targetWeight;

    /**
     * 达成目标体重的计划天数，必填且须为正整数
     */
    private Integer periodDays;

    /**
     * 每日建议摄入热量（大卡），可选；来自用户档案中的 dailyCalories
     */
    private BigDecimal dailyCalories;
}
