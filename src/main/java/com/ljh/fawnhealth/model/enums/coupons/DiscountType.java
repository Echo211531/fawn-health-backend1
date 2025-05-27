package com.ljh.fawnhealth.model.enums.coupons;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券折扣类型枚举
 * 定义了不同类型的优惠计算方式
 */
@Getter
@AllArgsConstructor
public enum DiscountType {
    /**
     * 每满减折扣
     * 例如：每满100元减20元
     */
    PER_PRICE_DISCOUNT(1, "每满减"),

    /**
     * 比例折扣
     * 例如：8折优惠，支付原价的80%
     */
    RATE_DISCOUNT(2, "折扣"),

    /**
     * 无门槛折扣
     * 无需满足任何条件即可使用的优惠
     */
    NO_THRESHOLD(3, "无门槛"),

    /**
     * 满减折扣
     * 例如：满200元减50元
     */
    PRICE_DISCOUNT(4, "满减");

    /**
     * 折扣类型值，用于数据库存储和前端传输
     */
    @JsonValue
    @EnumValue
    private final int value;

    /**
     * 折扣类型描述，用于前端展示和日志记录
     */
    private final String desc;

    /**
     * 根据值解析对应的折扣类型枚举
     * 用于JSON反序列化和参数解析
     *
     * @param value 折扣类型值
     * @return 对应的枚举实例，若未找到则返回null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static DiscountType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (DiscountType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}