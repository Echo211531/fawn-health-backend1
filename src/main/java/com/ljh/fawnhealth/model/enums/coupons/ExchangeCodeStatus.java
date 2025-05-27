package com.ljh.fawnhealth.model.enums.coupons;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 兑换码状态枚举
 * 定义了优惠券兑换码在其生命周期中的各种状态
 */
@Getter
@AllArgsConstructor
public enum ExchangeCodeStatus {
    /**
     * 待兑换状态
     * 兑换码已生成但尚未被用户使用，可进行兑换操作
     */
    UNUSED(1, "待兑换"),

    /**
     * 已兑换状态
     * 兑换码已被用户成功使用，不能再次兑换
     */
    USED(2, "已兑换"),

    /**
     * 已过期状态
     * 兑换码因超过有效期或活动结束，无法再进行兑换
     */
    EXPIRED(3, "兑换活动已结束");

    /**
     * 状态值，用于数据库存储和前端传输
     */
    @EnumValue
    @JsonValue
    private final int value;

    /**
     * 状态描述，用于前端展示和日志记录
     */
    private final String desc;

    /**
     * 根据状态值解析对应的枚举实例
     * 用于JSON反序列化和参数解析
     *
     * @param value 状态值
     * @return 对应的枚举实例，若未找到则返回null
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExchangeCodeStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (ExchangeCodeStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    /**
     * 根据状态值获取对应的描述信息
     *
     * @param value 状态值
     * @return 状态描述，若未找到则返回空字符串
     */
    public static String desc(Integer value) {
        ExchangeCodeStatus status = of(value);
        return status == null ? "" : status.desc;
    }
}