package com.zr.health.model.enums.order;

import lombok.Getter;

@Getter
public enum StatisticDimension {
    DAY_30("30天", "day"),
    WEEK("周", "week"),
    MONTH("月", "month"),
    YEAR("年", "year");

    private final String desc;
    private final String type;

    StatisticDimension(String desc, String type) {
        this.desc = desc;
        this.type = type;
    }

    // 确保包含这个匹配方法
    public static StatisticDimension match(String desc) {
        for (StatisticDimension dimension : values()) {
            if (dimension.desc.equals(desc)) {
                return dimension;
            }
        }
        return DAY_30; // 默认返回30天
    }
}