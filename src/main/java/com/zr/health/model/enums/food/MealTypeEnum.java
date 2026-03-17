package com.zr.health.model.enums.food;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 餐次类型枚举类
 * 对应表字段：meal_type（TINYINT类型）
 * 约定：1-4为基础餐次类型，100+为扩展类型（如特殊餐食类型）
 */
@Getter
@AllArgsConstructor
public enum MealTypeEnum {

    BREAKFAST(1, "早餐"),
    LUNCH(2, "午餐"),
    DINNER(3, "晚餐"),
    SNACK(4, "加餐");

    @JsonValue  // 序列化时返回value值
    @EnumValue  // MyBatis-Plus存储枚举值到数据库
    private final int value;

    private final String desc; // 描述信息（用于前端展示或日志）

    /**
     * 通过数值获取枚举实例（JSON反序列化专用）
     * @param value 数据库存储的数值或请求传入的数值
     * @return 枚举实例，未找到返回null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static MealTypeEnum of(Integer value) {
        if (value == null) {
            return null;
        }
        for (MealTypeEnum type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取所有枚举项的value和desc列表（用于前端下拉菜单等场景）
     * @return 包含"value"和"desc"字段的Map列表
     */
    public static List<Map<String, Object>> toList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MealTypeEnum type : values()) {
            result.add(Map.of("value", type.value, "desc", type.desc));
        }
        return result;
    }

    /**
     * 判断是否为基础餐次类型（非系统扩展类型）
     * @return true: 基础类型（value <= 4）；false: 扩展类型
     */
    public boolean isBaseType() {
        return value <= 4;
    }

    /**
     * 获取所有基础餐次类型
     * @return 包含早餐、午餐、晚餐、加餐的列表
     */
    public static List<MealTypeEnum> getBaseTypes() {
        return List.of(BREAKFAST, LUNCH, DINNER, SNACK);
    }
}