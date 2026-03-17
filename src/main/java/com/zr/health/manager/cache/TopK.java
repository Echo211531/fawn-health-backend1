package com.zr.health.manager.cache;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * TopK 接口定义了热点数据统计的核心操作
 * 实现类需提供高效的算法来识别和维护高频率访问的热点项
 */
public interface TopK {
    /**
     * 添加元素并更新计数
     * @param key 元素标识
     * @param increment 增量值，通常为1
     * @return 添加结果，包含是否成为热点、淘汰信息等
     */
    AddResult add(String key, int increment);

    /**
     * 获取当前跟踪的所有热点项
     * @return 热点项列表，包含键和计数
     */
    List<Item> list();

    /**
     * 获取被淘汰的热点项队列
     * @return 淘汰项的阻塞队列
     */
    BlockingQueue<Item> expelled();

    /**
     * 执行衰减操作，降低所有计数器的值
     * 模拟时间窗口，防止旧热点长期占据内存
     */
    void fading();

    /**
     * 获取总访问次数
     * @return 所有元素的累计访问次数
     */
    long total();

    /**
     * 获取TopK热点数据
     * @param k 需要获取的热点项数量
     * @return 热点项列表，按访问频率降序排列
     */
    List<Map.Entry<String, Long>> topK(int k);


    /**
     * 判断某个键是否为热点
     *
     * @param key 待判断的元素键
     * @return true: 是热点（在当前 TopK 列表中）false: 非热点
     */
    boolean isHotKey(String key);
}