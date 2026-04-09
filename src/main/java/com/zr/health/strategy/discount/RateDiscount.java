package com.zr.health.strategy.discount;

import com.zr.health.model.entity.Coupons;
import com.zr.health.utils.StringUtil;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@RequiredArgsConstructor
public class RateDiscount implements Discount {

    private static final String RULE_TEMPLATE = "满{}打{}折，上限{}";

    @Override
    public boolean canUse(int totalAmount, Coupons coupons) {
        return totalAmount >= coupons.getThresholdAmount() * 100;
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupons) {
        BigDecimal totalAmountBD = BigDecimal.valueOf(totalAmount);
        BigDecimal discountRate = BigDecimal.valueOf(coupons.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal payAmount = totalAmountBD.multiply(discountRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal calculatedDiscount = totalAmountBD.subtract(payAmount);

        BigDecimal maxDiscountAmount = coupons.getMaxDiscountAmount();
        if (maxDiscountAmount == null || maxDiscountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return calculatedDiscount.intValue();
        }
        return calculatedDiscount.min(maxDiscountAmount.multiply(BigDecimal.valueOf(100))).intValue();
    }

    @Override
    public String getRule(Coupons coupons) {
        BigDecimal max = coupons.getMaxDiscountAmount();
        return StringUtil.format(
                RULE_TEMPLATE,
                coupons.getThresholdAmount(),
                BigDecimal.valueOf(coupons.getDiscountValue()).movePointLeft(1).stripTrailingZeros().toPlainString(),
                (max == null || max.compareTo(BigDecimal.ZERO) <= 0) ? "不限" : max.stripTrailingZeros().toPlainString() + "元"
        );
    }
}
