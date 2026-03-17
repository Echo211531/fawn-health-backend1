package com.zr.health.manager.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SseEmitterManager {

    // 存储用户ID与SSE连接的映射（线程安全）
    private static final Map<Long, SseEmitter> USER_EMITTERS = new ConcurrentHashMap<>();

    // 存储用户最后一次活动时间（用于心跳检测和超时清理）
    private static final Map<Long, Long> USER_LAST_ACTIVE_TIME = new ConcurrentHashMap<>();


    /**
     * 创建用户的SSE连接
     * @param userId 用户ID
     * @return SSE连接对象
     */
    public SseEmitter createEmitter(Long userId) {
        // 若用户已存在连接，先关闭旧连接
        if (USER_EMITTERS.containsKey(userId)) {
            closeEmitter(userId);
        }

        // 创建新连接（设置30分钟超时）
        SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        // 注册连接生命周期回调
        registerEmitterCallbacks(userId, emitter);

        // 存储连接和活动时间
        USER_EMITTERS.put(userId, emitter);
        USER_LAST_ACTIVE_TIME.put(userId, System.currentTimeMillis());

        log.info("SSE连接创建成功，userId={}, 当前连接数={}", userId, USER_EMITTERS.size());
        return emitter;
    }


    /**
     * 注册SSE连接的生命周期回调（完成、超时、错误）
     */
    private void registerEmitterCallbacks(Long userId, SseEmitter emitter) {
        // 连接完成（客户端主动关闭）
        emitter.onCompletion(() -> {
            log.info("SSE连接完成，userId={}", userId);
            removeEmitter(userId);
        });

        // 连接超时
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，userId={}", userId);
            removeEmitter(userId);
        });

        // 连接错误
        emitter.onError(e -> {
            log.error("SSE连接错误，userId={}", userId, e);
            removeEmitter(userId);
        });
    }


    /**
     * 向指定用户推送消息
     * @param userId 用户ID
     * @param eventName 事件名称（前端可根据名称区分消息类型）
     * @param data 推送的数据（会自动序列化为JSON）
     * @param <T> 数据类型
     * @return 是否推送成功
     */
    public <T> boolean sendMessage(Long userId, String eventName, T data) {
        if (userId == null || eventName == null || data == null) {
            log.warn("SSE推送参数无效，userId={}, eventName={}", userId, eventName);
            return false;
        }

        SseEmitter emitter = USER_EMITTERS.get(userId);
        if (emitter == null) {
            log.warn("SSE推送失败：用户无有效连接，userId={}", userId);
            return false;
        }

        try {
            // 发送带事件名称的消息
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));

            // 更新活动时间
            USER_LAST_ACTIVE_TIME.put(userId, System.currentTimeMillis());
            log.debug("SSE消息推送成功，userId={}, eventName={}", userId, eventName);
            return true;
        } catch (IOException e) {
            log.error("SSE消息推送失败，userId={}, eventName={}", userId, eventName, e);
            removeEmitter(userId); // 推送失败时移除无效连接
            return false;
        }
    }


    /**
     * 向指定用户推送心跳消息（防止连接被防火墙断开）
     * @param userId 用户ID
     */
    public void sendHeartbeat(Long userId) {
        sendMessage(userId, "heartbeat", Map.of("timestamp", System.currentTimeMillis()));
    }


    /**
     * 批量推送消息给多个用户
     * @param userIds 用户ID集合
     * @param eventName 事件名称
     * @param data 推送的数据
     * @param <T> 数据类型
     * @return 推送成功的用户数
     */
    public <T> int batchSendMessage(Set<Long> userIds, String eventName, T data) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        return (int) userIds.stream()
                .filter(userId -> sendMessage(userId, eventName, data))
                .count();
    }


    /**
     * 移除用户的SSE连接（主动关闭连接）
     * @param userId 用户ID
     */
    public void removeEmitter(Long userId) {
        if (userId == null) {
            return;
        }

        // 关闭连接
        closeEmitter(userId);

        // 清理映射
        USER_EMITTERS.remove(userId);
        USER_LAST_ACTIVE_TIME.remove(userId);

        log.info("SSE连接已移除，userId={}, 当前连接数={}", userId, USER_EMITTERS.size());
    }


    /**
     * 关闭用户的SSE连接（发送完成信号）
     */
    private void closeEmitter(Long userId) {
        SseEmitter emitter = USER_EMITTERS.get(userId);
        if (emitter != null) {
            try {
                emitter.complete(); // 发送连接完成信号
            } catch (Exception e) {
                log.error("关闭SSE连接失败，userId={}", userId, e);
            }
        }
    }


    /**
     * 清理超时未活动的连接（可定时任务调用）
     * @param timeoutMillis 超时时间（毫秒），默认30分钟
     * @return 清理的连接数
     */
    public int cleanTimeoutConnections(long timeoutMillis) {
        long now = System.currentTimeMillis();
        // 筛选超时的用户ID
        Set<Long> timeoutUserIds = USER_LAST_ACTIVE_TIME.entrySet().stream()
                .filter(entry -> now - entry.getValue() > timeoutMillis)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        // 批量移除超时连接
        timeoutUserIds.forEach(this::removeEmitter);
        log.info("清理超时SSE连接完成，清理数量={}, 剩余连接数={}", timeoutUserIds.size(), USER_EMITTERS.size());
        return timeoutUserIds.size();
    }


    /**
     * 获取当前活跃的用户连接ID
     */
    public Set<Long> getActiveUserIds() {
        return USER_EMITTERS.keySet();
    }


    /**
     * 获取当前连接总数
     */
    public int getConnectionCount() {
        return USER_EMITTERS.size();
    }


    /**
     * 检查用户是否存在有效连接
     */
    public boolean hasActiveConnection(Long userId) {
        return USER_EMITTERS.containsKey(userId);
    }
}