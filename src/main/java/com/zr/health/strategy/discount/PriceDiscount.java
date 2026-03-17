package com.zr.health.strategy.discount;

import com.zr.health.model.entity.Coupons;

import com.zr.health.utils.NumberUtil;
import com.zr.health.utils.StringUtil;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PriceDiscount implements Discount{

    private static final String RULE_TEMPLATE = "满{}减{}";


    @Override
    public boolean canUse(int totalAmount, Coupons coupons) {
        return totalAmount >= coupons.getThresholdAmount();
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupons) {
        return coupons.getDiscountValue();
    }

    @Override
    public String getRule(Coupons coupons) {
        return StringUtil.format(
                RULE_TEMPLATE,
                NumberUtil.scaleToStr(coupons.getThresholdAmount(), 2),
                NumberUtil.scaleToStr(coupons.getDiscountValue(), 2)
        );
    }
}
