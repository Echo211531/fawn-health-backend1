package com.ljh.fawnhealth.manager.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存管理器 - 提供多级缓存和热点数据探测功能
 * 1. 使用 Caffeine 实现本地缓存
 * 2. 使用 Redis 实现分布式缓存
 * 3. 使用 HeavyKeeper 算法实现热点数据探测
 */
@Component
@Slf4j
public class CacheManager {

    private TopK hotKeyDetector;      // 热点键检测器，基于HeavyKeeper算法
    private Cache<String, Object> localCache;  // 本地缓存，使用Caffeine实现

    @Resource
    private RedisTemplate<String, Object> redisTemplate;  // Redis操作模板

    /**
     * 初始化热点键检测器
     * @return TopK接口实现
     */
    @Bean
    public TopK getHotKeyDetector() {
        hotKeyDetector = new HeavyKeeper(
                100,      // 跟踪Top 100热点键
                100000,   // 哈希表宽度，影响冲突概率
                5,        // 哈希表深度，多个哈希函数并行
                0.92,     // 衰减系数，控制旧数据遗忘速度
                1        // 最小计数阈值，达到10次才被视为热点
        );
        return hotKeyDetector;
    }

    /**
     * 初始化本地缓存
     * @return Caffeine缓存实例
     */
    @Bean
    public Cache<String, Object> localCache() {
        return localCache = Caffeine.newBuilder()
                .maximumSize(1000)          // 最大缓存1000个条目
                .expireAfterWrite(5, TimeUnit.MINUTES)  // 写入后5分钟过期
                .build();
    }

    /**
     * 构造复合缓存键
     * @param hashKey 哈希键前缀
     * @param key 具体键
     * @return 复合键（格式：hashKey:key）
     */
    private String buildCacheKey(String hashKey, String key) {
        return hashKey + ":" + key;
    }

    /**
     * 从缓存获取数据（支持多级缓存）
     * 1. 先查本地缓存
     * 2. 本地未命中则查Redis
     * 3. 记录访问频率，自动识别热点数据
     * @param hashKey 哈希键前缀
     * @param key 具体键
     * @return 缓存值，或null（未找到）
     */
    public Object get(String hashKey, String key) {
        String compositeKey = buildCacheKey(hashKey, key);

        // 1. 优先查询本地缓存
        Object value = localCache.getIfPresent(compositeKey);
        if (value != null) {
            log.info("本地缓存命中: {}", compositeKey);
            hotKeyDetector.add(key, 1);  // 记录访问计数
            return value;
        }

        // 2. 本地缓存未命中，查询Redis
        Object redisValue = redisTemplate.opsForHash().get(hashKey, key);
        if (redisValue == null) {
            return null;
        }

        // 新增日志
        log.info("Redis命中，计数+1，key: {}", key);
        // 3. 记录访问频率
        AddResult addResult = hotKeyDetector.add(key, 1);

        // 4. 如果是热点数据，缓存到本地
        if (addResult.isHotKey()) {
            localCache.put(compositeKey, redisValue);
            log.info("热点数据已缓存到本地: {}", compositeKey);
        }

        return redisValue;
    }

    /**
     * 更新本地缓存（仅当缓存已存在时）
     * @param hashKey 哈希键前缀
     * @param key 具体键
     * @param value 新值
     */
    public void putIfPresent(String hashKey, String key, Object value) {
        String compositeKey = buildCacheKey(hashKey, key);
        if (localCache.getIfPresent(compositeKey) != null) {
            localCache.put(compositeKey, value);
            log.info("本地缓存已更新: {}", compositeKey);
        }
    }

    /**
     * 更新缓存（同时更新本地缓存和Redis）
     * @param hashKey 哈希键前缀
     * @param key 具体键
     * @param value 新值
     * @param timeout 超时时间（秒）
     */
    public void put(String hashKey, String key, Object value, long timeout) {
        log.info("写入缓存: hashKey={}, key={}, timeout={}", hashKey, key, timeout);
        String compositeKey = buildCacheKey(hashKey, key);

        // 更新Redis缓存
        redisTemplate.opsForHash().put(hashKey, key, value);
        if (timeout > 0) {
            redisTemplate.expire(hashKey, timeout, TimeUnit.SECONDS);
        }

        // 如果是热点数据，更新本地缓存
        if (hotKeyDetector.isHotKey(key)) {
            localCache.put(compositeKey, value);
            log.info("热点数据已更新本地缓存: {}", compositeKey);
        }
    }

    /**
     * 获取热点键列表
     * @param topN 需要获取的热点数量
     * @return 热点键列表（按热度降序）
     */
    public List<String> getHotKeys(int topN) {
        return hotKeyDetector.topK(topN).stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }


    /**
     * 删除缓存（同时删除本地缓存和 Redis 缓存）
     * @param hashKey 哈希键前缀
     * @param key 具体键
     */
    public void delete(String hashKey, String key) {
        String compositeKey = buildCacheKey(hashKey, key);
        log.info("删除缓存: hashKey={}, key={}", hashKey, key);

        // 删除本地缓存
        localCache.invalidate(compositeKey);

        // 删除 Redis 缓存
        redisTemplate.opsForHash().delete(hashKey, key);

//        // 从热点键集合中移除（可选：如果热点键不再需要跟踪）
//        if (hotKeyDetector.isHotKey(key)) {
//            hotKeyDetector.expelledQueue().offer(new Item(key, 0)); // 触发淘汰逻辑
//        }
    }

    /**
     * 定时衰减热点数据计数
     * 每20秒执行一次，防止旧热点长期占据内存
     */
    @Scheduled(fixedRate = 20, timeUnit = TimeUnit.SECONDS)
    public void cleanHotKeys() {
        hotKeyDetector.fading();
        log.debug("热点数据已衰减");
    }
}