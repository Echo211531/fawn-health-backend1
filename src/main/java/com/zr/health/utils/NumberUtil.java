package com.zr.health.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtil {
    /**
     * 将数字格式化为指定的小数位数。
     *
     * @param value 要格式化的数值
     * @param scale 小数位数
     * @return 格式化后的数字字符串
     */
    public static String scaleToStr(int value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toString();
    }

    /**
     * 重载方法，支持 BigDecimal 类型
     *
     * @param value 要格式化的 BigDecimal 数值
     * @param scale 小数位数
     * @return 格式化后的数字字符串
     */
    public static String scaleToStr(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP).toString();
    }

    /**
     * 重载方法，支持 double 类型
     *
     * @param value 要格式化的 double 数值
     * @param scale 小数位数
     * @return 格式化后的数字字符串
     */
    public static String scaleToStr(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toString();
    }
}
