package com.zr.health.model.dto.food;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 饮食记录添加响应DTO
 * 用于返回饮食记录添加操作的结果信息，包含操作状态和计算的总卡路里
 */
@Data
public class DietRecordAddResponse {
    /**
     * 操作结果状态
     * true-添加成功，false-添加失败
     */
    private Boolean success;

    /**
     * 本次饮食记录的总卡路里
     * 基于食物项列表中各食物的卡路里值和摄入量计算得出
     * 单位：千卡(kcal)
     */
    private BigDecimal totalCalories;
}