package com.zr.health.manager.cache;

/**
 * 表示热点数据项的不可变数据结构
 * 用于存储元素标识及其访问计数
 */
public record Item(String key, int count) {
    // 记录类自动生成以下内容：
    // - 字段：key, count
    // - 构造方法：Item(String key, int count)
    // - 访问器：key(), count()
    // - equals(), hashCode(), toString() 方法
}