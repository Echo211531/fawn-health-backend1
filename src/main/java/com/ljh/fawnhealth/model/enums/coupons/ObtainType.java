package com.ljh.fawnhealth.model.enums.coupons;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券获取方式枚举
 * 定义了用户获取优惠券的不同途径
 */
@Getter
@AllArgsConstructor
public enum ObtainType {
    /**
     * 手动领取方式
     * 用户在优惠券中心主动领取，通常有数量限制
     */
    PUBLIC(1, "手动领取"),

    /**
     * 兑换码发放方式
     * 通过系统生成的兑换码获取优惠券，常用于活动推广
     */
    ISSUE(2, "发放兑换码");

    /**
     * 获取方式值，用于数据库存储和前端传输
     */
    @EnumValue
    @JsonValue
    private final int value;

    /**
     * 获取方式描述，用于前端展示和日志记录
     */
    private final String desc;

    /**
     * 根据值解析对应的获取方式枚举
     * 用于JSON反序列化和参数解析
     *
     * @param value 获取方式值
     * @return 对应的枚举实例，若未找到则返回null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ObtainType of(Integer value) {
        if (value == null) {
            return null;
        }
        for (ObtainType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return null;
    }
}