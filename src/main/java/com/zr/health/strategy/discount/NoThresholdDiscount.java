package com.zr.health.strategy.discount;


import com.zr.health.model.entity.Coupons;
import com.zr.health.utils.StringUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NoThresholdDiscount implements Discount{

    private static final String RULE_TEMPLATE = "无门槛抵{}元";

    @Override
    public boolean canUse(int totalAmount, Coupons coupon) {
        return totalAmount > coupon.getDiscountValue() * 100;
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupon) {
        return coupon.getDiscountValue() * 100;
    }

    @Override
    public String getRule(Coupons coupon) {
        return StringUtil.format(RULE_TEMPLATE, coupon.getDiscountValue());
    }
}
