package com.ljh.fawnhealth.strategy.discount;

import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.utils.NumberUtil;
import com.ljh.fawnhealth.utils.StringUtil;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class RateDiscount implements Discount {

    private static final String RULE_TEMPLATE = "满{}打{}折，上限{}元";

    @Override
    public boolean canUse(int totalAmount, Coupons coupons) {
        return totalAmount >= coupons.getThresholdAmount();
    }

    @Override
    public int calculateDiscount(int totalAmount, Coupons coupons) {
        // 使用 BigDecimal 处理计算
        BigDecimal totalAmountBD = BigDecimal.valueOf(totalAmount);
        BigDecimal discountRate = BigDecimal.valueOf(coupons.getDiscountValue()).divide(BigDecimal.valueOf(100)); // 折扣率
        BigDecimal calculatedDiscount = totalAmountBD.multiply(BigDecimal.ONE.subtract(discountRate)); // 计算折扣金额

        // 获取 maxDiscountAmount，确保不为 null
        BigDecimal maxDiscountAmount = coupons.getMaxDiscountAmount() != null ? coupons.getMaxDiscountAmount() : BigDecimal.ZERO;

        // 返回最小折扣金额（即最大折扣金额的上限）
        return calculatedDiscount.min(maxDiscountAmount).intValue();
    }

    @Override
    public String getRule(Coupons coupons) {
        return StringUtil.format(
                RULE_TEMPLATE,
                NumberUtil.scaleToStr(coupons.getThresholdAmount(), 2),
                NumberUtil.scaleToStr(coupons.getDiscountValue(), 1),
                NumberUtil.scaleToStr(coupons.getMaxDiscountAmount(), 2)
        );
    }
}
