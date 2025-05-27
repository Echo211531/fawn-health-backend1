package com.ljh.fawnhealth.manager.cache;

import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.core.util.HashUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * HeavyKeeper 算法实现 - 用于高效识别和跟踪热点数据
 * 基于 Count-Min Sketch 概率数据结构，结合衰减机制，在有限内存中近似统计高频项
 */
@Slf4j
public class HeavyKeeper implements TopK {
    private static final int LOOKUP_TABLE_SIZE = 256;
    private final int k;                // 要跟踪的热点项数量
    private final int width;            // 每个哈希表的宽度（桶数量）
    private final int depth;            // 哈希表的深度（并行使用的哈希函数数量）
    private final double[] lookupTable; // 衰减因子查找表，加速计算
    private final Bucket[][] buckets;   // 二维数组存储所有桶的计数器
    private final PriorityQueue<Node> minHeap; // 最小堆，维护热点项
    private final BlockingQueue<Item> expelledQueue; // 淘汰项队列
    private final Random random;        // 随机数生成器，用于概率性衰减
    private long total;                 // 总访问次数计数器
    private final int minCount;         // 最小计数阈值，达到该值才会被视为热点
    private final Set<String> hotKeys = new ConcurrentHashSet<>(); // 新增：热点键集合

    /**
     * 构造函数，初始化 HeavyKeeper 实例
     * @param k 跟踪的热点项数量
     * @param width 哈希表宽度
     * @param depth 哈希表深度
     * @param decay 衰减因子，控制旧数据的遗忘速度
     * @param minCount 最小计数阈值
     */
    public HeavyKeeper(int k, int width, int depth, double decay, int minCount) {
        this.k = k;
        this.width = width;
        this.depth = depth;
        this.minCount = minCount;

        // 初始化衰减因子查找表，预计算常用的衰减值
        this.lookupTable = new double[LOOKUP_TABLE_SIZE];
        for (int i = 0; i < LOOKUP_TABLE_SIZE; i++) {
            lookupTable[i] = Math.pow(decay, i);
        }

        // 初始化桶数组
        this.buckets = new Bucket[depth][width];
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                buckets[i][j] = new Bucket();
            }
        }

        // 初始化最小堆，用于维护热点项
        this.minHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
        this.expelledQueue = new LinkedBlockingQueue<>();
        this.random = new Random();
        this.total = 0;
    }

    /**
     * 添加元素并更新计数
     * @param key 元素键
     * @param increment 增量值
     * @return 添加结果，包含是否为热点及淘汰信息
     */
    @Override
    public AddResult add(String key, int increment) {
        byte[] keyBytes = key.getBytes();
        long itemFingerprint = hash(keyBytes); // 计算指纹用于快速比较
        int maxCount = 0;

        // 对每个哈希表进行更新
        for (int i = 0; i < depth; i++) {
            int bucketNumber = Math.abs(hash(keyBytes)) % width; // 计算桶索引
            Bucket bucket = buckets[i][bucketNumber];

            synchronized (bucket) {
                if (bucket.count == 0) {
                    // 桶为空，直接记录新元素
                    bucket.fingerprint = itemFingerprint;
                    bucket.count = increment;
                    maxCount = Math.max(maxCount, increment);
                } else if (bucket.fingerprint == itemFingerprint) {
                    // 桶中已有该元素，增加计数
                    bucket.count += increment;
                    maxCount = Math.max(maxCount, bucket.count);
                } else {
                    // 桶中存在其他元素，进行概率性衰减
                    for (int j = 0; j < increment; j++) {
                        double decay = bucket.count < LOOKUP_TABLE_SIZE ?
                                lookupTable[bucket.count] :
                                lookupTable[LOOKUP_TABLE_SIZE - 1];
                        if (random.nextDouble() < decay) {
                            bucket.count--;
                            if (bucket.count == 0) {
                                // 原元素被淘汰，记录新元素
                                bucket.fingerprint = itemFingerprint;
                                bucket.count = increment - j;
                                maxCount = Math.max(maxCount, bucket.count);
                                break;
                            }
                        }
                    }
                }
            }
        }

        total += increment; // 更新总计数

        // 如果计数未达到最小阈值，不视为热点
        if (maxCount < minCount) {
            return new AddResult(null, false, null);
        }

        // 更新热点堆和热点键集合
        synchronized (minHeap) {
            boolean isHot = false;
            String expelled = null;

            // 检查元素是否已在热点堆中
            Optional<Node> existing = minHeap.stream()
                    .filter(n -> n.key.equals(key))
                    .findFirst();

            if (existing.isPresent()) {
                // 已在堆中，更新计数
                minHeap.remove(existing.get());
                minHeap.add(new Node(key, maxCount));
                isHot = true; // 保留在热点集合中
            } else {
                // 不在堆中，判断是否有足够热度加入
                if (minHeap.size() < k || maxCount >= (minHeap.peek() != null ? minHeap.peek().count : 0)) {
                    Node newNode = new Node(key, maxCount);
                    if (minHeap.size() >= k) {
                        // 堆已满，淘汰热度最低的元素
                        expelled = minHeap.poll().key;
                        expelledQueue.offer(new Item(expelled, maxCount));
                        hotKeys.remove(expelled); // 淘汰键时从集合移除
                    }
                    minHeap.add(newNode);
                    hotKeys.add(key); // 新键加入堆时，添加到集合
                    isHot = true;
                }
            }

            return new AddResult(expelled, isHot, key);
        }
    }

    /**
     * 获取当前跟踪的所有热点项
     * @return 热点项列表，按热度降序排列
     */
    @Override
    public List<Item> list() {
        synchronized (minHeap) {
            List<Item> result = new ArrayList<>(minHeap.size());
            for (Node node : minHeap) {
                result.add(new Item(node.key, node.count));
            }
            result.sort((a, b) -> Integer.compare(b.count(), a.count())); // 按热度降序排列
            return result;
        }
    }

    /**
     * 获取被淘汰的热点项队列
     * @return 淘汰项队列
     */
    @Override
    public BlockingQueue<Item> expelled() {
        return expelledQueue;
    }

    /**
     * 执行衰减操作，降低所有计数器的值
     * 模拟时间窗口，防止旧热点长期占据内存
     */
    @Override
    public void fading() {
        // 衰减所有桶的计数
        for (Bucket[] row : buckets) {
            for (Bucket bucket : row) {
                synchronized (bucket) {
                    bucket.count = bucket.count >> 1; // 减半操作，相当于乘以0.5
                }
            }
        }

        // 衰减热点堆中的所有元素
        synchronized (minHeap) {
            PriorityQueue<Node> newHeap = new PriorityQueue<>(Comparator.comparingInt(n -> n.count));
            for (Node node : minHeap) {
                newHeap.add(new Node(node.key, node.count >> 1)); // 计数减半
            }
            minHeap.clear();
            minHeap.addAll(newHeap);

            // 衰减后，重新计算热点键集合（可选：防止过时键残留）
            hotKeys.clear();
            hotKeys.addAll(minHeap.stream().map(Node::getKey).collect(Collectors.toSet()));
        }

        // 总计数减半
        total = total >> 1;
    }

    /**
     * 获取总访问次数
     * @return 总访问次数
     */
    @Override
    public long total() {
        return total;
    }

    /**
     * 获取TopK热点项
     * @param k 需要获取的热点项数量
     * @return 热点项列表，按热度降序排列
     */
    @Override
    public List<Map.Entry<String, Long>> topK(int k) {
        synchronized (minHeap) {
            // 从最小堆中提取元素并转换为所需格式
            return minHeap.stream()
                    .sorted(Comparator.comparingInt((Node n) -> n.count).reversed()) // 按计数降序
                    .limit(k)
                    .map(n -> new AbstractMap.SimpleEntry<String, Long>(n.key, (long) n.count))
                    .collect(Collectors.toList());
        }
    }

    @Override
    public boolean isHotKey(String key) {
        boolean exists = hotKeys.contains(key);
        log.debug("isHotKey({}) = {}", key, exists);
        return hotKeys.contains(key); // 通过集合快速判断
    }

    /**
     * 内部类：表示哈希表中的一个桶
     */
    private static class Bucket {
        long fingerprint; // 元素指纹，用于快速比较
        int count;        // 计数
    }

    /**
     * 内部类：表示热点堆中的一个节点
     */
    @Data
    private static class Node {
        final String key; // 元素键
        final int count;  // 计数

        Node(String key, int count) {
            this.key = key;
            this.count = count;
        }
    }

    /**
     * 计算字节数组的哈希值
     * @param data 输入数据
     * @return 哈希值
     */
    private static int hash(byte[] data) {
        return HashUtil.murmur32(data); // 使用Hutool工具包的Murmur32哈希算法
    }
}