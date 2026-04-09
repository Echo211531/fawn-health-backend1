package com.zr.health.strategy.discount;


import com.zr.health.model.entity.Coupons;
import com.zr.health.utils.StringUtil;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class PerPriceDiscount implements Discount {

    private final static String RULE_TEMPLATE = "每满{}减{}，上限{}";

    @Override
    public boolean canUse(int totalAmount, Coupons coupons) {
        return totalAmount >= coupons.getThresholdAmount() * 100;
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupons) {
        int discount = 0;
        Integer thresholdAmount = coupons.getThresholdAmount() * 100;
        Integer discountValue = coupons.getDiscountValue() * 100;
        while (totalAmount >= thresholdAmount) {
            discount += discountValue;
            totalAmount -= thresholdAmount;
        }
        BigDecimal maxDiscountAmount = coupons.getMaxDiscountAmount();
        if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return discount;
        }
        return BigDecimal.valueOf(discount).min(maxDiscountAmount.multiply(BigDecimal.valueOf(100))).intValue();
    }

    @Override
    public String getRule(Coupons coupon) {
        BigDecimal max = coupon.getMaxDiscountAmount();
        return StringUtil.format(
                RULE_TEMPLATE,
                coupon.getThresholdAmount(),
                coupon.getDiscountValue(),
                (max == null || max.compareTo(BigDecimal.ZERO) <= 0) ? "不限" : max.stripTrailingZeros().toPlainString());
    }
}
