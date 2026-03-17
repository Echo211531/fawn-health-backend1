package com.zr.health.model.dto.food;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NutritionAnalysisResult {
    private NutritionData averageDailyNutrition;
    private BigDecimal calorieBalance;
    private Integer nutritionScore;
}