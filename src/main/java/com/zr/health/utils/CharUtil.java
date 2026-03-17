package com.zr.health.utils;

import java.util.Random;

/**
 * 字符处理工具类，提供与字符相关的处理方法，
 * 当前包含生成随机数字字符串的功能。
 */
public class CharUtil {

    /**
     * 生成一个 6 位的随机数字字符串，通常可用于验证码等场景。
     *
     * @return 一个长度为 6 的随机数字字符串，由 0 到 9 的数字组成。
     *         例如："123456"、"987654" 等。
     */
    public static String randomVerify() {
        // 创建一个 Random 实例，用于生成随机数
        Random random = new Random();
        // 用于存储生成的随机数字字符串
        String result = "";
        // 循环 6 次，生成 6 位随机数字
        for (int i = 0; i < 6; i++) {
            // 生成一个 0 到 9 之间的随机整数，并将其转换为字符串后拼接到 result 中
            result += random.nextInt(10);
        }
        // 返回生成的 6 位随机数字字符串
        return result;
    }
}