package com.zr.health.model.dto.food;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class NutritionData {
    private BigDecimal calories = BigDecimal.ZERO;
    private BigDecimal protein = BigDecimal.ZERO;
    private BigDecimal fat = BigDecimal.ZERO;
    private BigDecimal carbohydrate = BigDecimal.ZERO;

    // 添加全参数构造函数
    public NutritionData() {}

    public NutritionData(BigDecimal calories, BigDecimal protein,
                         BigDecimal fat, BigDecimal carbohydrate) {
        this.calories = calories != null ? calories : BigDecimal.ZERO;
        this.protein = protein != null ? protein : BigDecimal.ZERO;
        this.fat = fat != null ? fat : BigDecimal.ZERO;
        this.carbohydrate = carbohydrate != null ? carbohydrate : BigDecimal.ZERO;
    }

    public NutritionData add(NutritionData other) {
        NutritionData result = new NutritionData();
        result.setCalories(this.calories.add(other.getCalories()));
        result.setProtein(this.protein.add(other.getProtein()));
        result.setFat(this.fat.add(other.getFat()));
        result.setCarbohydrate(this.carbohydrate.add(other.getCarbohydrate()));
        return result;
    }

    public NutritionData divide(long divisor) {
        if (divisor == 0) return this;
        NutritionData result = new NutritionData();
        result.setCalories(this.calories.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        result.setProtein(this.protein.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        result.setFat(this.fat.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        result.setCarbohydrate(this.carbohydrate.divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP));
        return result;
    }
}
