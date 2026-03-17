package com.zr.health.strategy.discount;


import com.zr.health.model.entity.Coupons;

/**
 * <p>优惠券折扣功能接口</p>
 */
public interface Discount {
    /**
     * 判断当前价格是否满足优惠券使用限制
     * @param totalAmount 订单总价
     * @param coupon 优惠券信息
     * @return 是否可以使用优惠券
     */
    boolean canUse(int totalAmount, Coupons coupon);

    /**
     * 计算折扣金额
     * @param totalAmount 总金额
     * @param coupon 优惠券信息
     * @return 折扣金额
     */
    int calculateDiscount(int totalAmount, Coupons coupon);

    /**
     * 根据优惠券规则返回规则描述信息
     * @return 规则描述信息
     */
    String getRule(Coupons coupon);
}
