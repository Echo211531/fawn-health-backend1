package com.zr.health.utils;

public class StringUtil {

    /**
     * 使用提供的参数格式化字符串。
     *
     * @param template 字符串模板，包含占位符
     * @param args 要替换占位符的参数
     * @return 格式化后的字符串
     */
    public static String format(String template, Object... args) {
        return String.format(template, args);
    }
}
