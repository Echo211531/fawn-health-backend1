package com.ljh.fawnhealth.strategy.discount;


import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.utils.NumberUtil;
import com.ljh.fawnhealth.utils.StringUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class NoThresholdDiscount implements Discount{

    private static final String RULE_TEMPLATE = "无门槛抵{}元";

    @Override
    public boolean canUse(int totalAmount, Coupons coupon) {
        return totalAmount > coupon.getDiscountValue();
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupon) {
        return coupon.getDiscountValue();
    }

    @Override
    public String getRule(Coupons coupon) {
        return StringUtil.format(RULE_TEMPLATE, NumberUtil.scaleToStr(coupon.getDiscountValue(), 2));
    }
}
