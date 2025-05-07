package com.ljh.fawnhealth.strategy.discount;


import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.utils.NumberUtil;
import com.ljh.fawnhealth.utils.StringUtil;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class PerPriceDiscount implements Discount {

    private final static String RULE_TEMPLATE = "每满{}减{}，上限{}";

    @Override
    public boolean canUse(int totalAmount, Coupons coupons) {
        return totalAmount >= coupons.getThresholdAmount();
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupons) {
        int discount = 0;
        Integer thresholdAmount = coupons.getThresholdAmount();
        Integer discountValue = coupons.getDiscountValue();
        while (totalAmount >= thresholdAmount) {
            discount += discountValue;
            totalAmount -= thresholdAmount;
        }
        // 将计算的 discount 转换为 BigDecimal 进行比较
        BigDecimal calculatedDiscount = BigDecimal.valueOf(discount);

        // 比较并返回最小的值
        return calculatedDiscount.compareTo(coupons.getMaxDiscountAmount()) > 0 ? coupons.getMaxDiscountAmount().intValue() : calculatedDiscount.intValue();
    }

    @Override
    public String getRule(Coupons coupon) {
        return StringUtil.format(
                RULE_TEMPLATE,
                NumberUtil.scaleToStr(coupon.getThresholdAmount(), 2),
                NumberUtil.scaleToStr(coupon.getDiscountValue(), 2),
                NumberUtil.scaleToStr(coupon.getMaxDiscountAmount(), 2));
    }
}
