package com.zr.health.model.enums.rule;

/**
 * 营养类型枚举
 */
public enum NutritionType {
    CALORIES("热量", "kcal"),
    PROTEIN("蛋白质", "g"),
    FAT("脂肪", "g"),
    CARBOHYDRATE("碳水化合物", "g"),
    VEGETABLES("蔬菜", "g");

    private final String name;
    private final String unit;

    NutritionType(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }
}