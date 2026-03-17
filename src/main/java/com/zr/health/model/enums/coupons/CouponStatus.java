package com.zr.health.model.enums.coupons;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 优惠券状态枚举
 * 定义了优惠券在其生命周期中的各种状态
 */
@Getter
@AllArgsConstructor
public enum CouponStatus {
    /**
     * 草稿状态
     * 优惠券已创建但尚未完成配置，暂不参与发放
     */
    DRAFT(1, "待发放"),

    /**
     * 未开始状态
     * 优惠券已完成配置但尚未到达设定的发放开始时间
     */
    UN_ISSUE(2, "未开始"),

    /**
     * 发放中状态
     * 优惠券正在进行发放，用户可以领取
     */
    ISSUING(3, "发放中"),

    /**
     * 发放结束状态
     * 优惠券已到达设定的发放结束时间或已发放完毕
     */
    FINISHED(4, "发放结束"),

    /**
     * 暂停状态
     * 优惠券因特殊原因暂时停止发放，后续可恢复
     */
    PAUSE(5, "暂停");

    /**
     * 状态值，用于数据库存储和前端传输
     */
    @JsonValue
    @EnumValue
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
    public static CouponStatus of(Integer value) {
        if (value == null) {
            return null;
        }
        for (CouponStatus status : values()) {
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
        CouponStatus status = of(value);
        return status == null ? "" : status.desc;
    }
}