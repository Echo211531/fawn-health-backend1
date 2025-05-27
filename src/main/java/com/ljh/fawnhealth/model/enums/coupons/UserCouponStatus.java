package com.ljh.fawnhealth.model.enums.coupons;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户优惠券状态枚举
 * 定义了用户持有的优惠券在使用过程中的各种状态
 */
@Getter
@AllArgsConstructor
public enum UserCouponStatus {
    /**
     * 未使用状态
     * 优惠券已发放给用户，尚未被使用，且在有效期内
     */
    UNUSED(1, "未使用"),

    /**
     * 已使用状态
     * 优惠券已被用户使用，不能再次使用
     */
    USED(2, "已使用"),

    /**
     * 已过期状态
     * 优惠券因超过有效期或被系统取消，无法再使用
     */
    EXPIRED(3, "已过期");

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
    public static UserCouponStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (UserCouponStatus status : values()) {
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
        UserCouponStatus status = of(value);
        return status == null ? "" : status.desc;
    }
}