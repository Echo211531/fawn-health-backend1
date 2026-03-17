package com.zr.health.ai.store;

import com.zr.health.ai.model.ChatStreamEvent;
import com.zr.health.ai.model.StreamEvent;
import com.zr.health.ai.model.StreamEventType;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 聊天流式事件存储管理器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamEventStore {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 保存流式事件
     *
     * @param conversationId 会话ID
     * @param streamEvent    流式事件
     * @param userId         用户ID
     */
    public void saveStreamEvent(String conversationId, StreamEvent streamEvent, String userId) {
        try {
            // 获取当前会话的事件数量，用作sequence
            int sequence = getEventCountByConversationId(conversationId);

            ChatStreamEvent chatStreamEvent = ChatStreamEvent.fromStreamEvent(
                    conversationId, streamEvent, sequence + 1, userId);

            mongoTemplate.insert(chatStreamEvent);
            log.debug("保存流式事件成功: conversationId={}, type={}, sequence={}",
                    conversationId, streamEvent.getType(), sequence + 1);
        } catch (Exception e) {
            log.error("保存流式事件失败: conversationId={}, type={}",
                    conversationId, streamEvent.getType(), e);
        }
    }

    /**
     * 获取会话的所有流式事件
     *
     * @param conversationId 会话ID
     * @return 流式事件列表
     */
    public List<ChatStreamEvent> getStreamEvents(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.ASC, "sequence"));

        return mongoTemplate.find(query, ChatStreamEvent.class);
    }

    /**
     * 获取会话的流式事件（限制数量）
     *
     * @param conversationId 会话ID
     * @param lastN          最近N条
     * @return 流式事件列表
     */
    public List<ChatStreamEvent> getStreamEvents(String conversationId, int lastN) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = new Query(Criteria.where("conversationId").is(conversationId))
                .with(Sort.by(Sort.Direction.DESC, "sequence"))
                .limit(lastN);

        List<ChatStreamEvent> events = mongoTemplate.find(query, ChatStreamEvent.class);
        // 反转顺序，使其按sequence正序返回
        Collections.reverse(events);
        return events;
    }

    /**
     * 按事件类型获取流式事件
     *
     * @param conversationId 会话ID
     * @param types          事件类型列表
     * @return 流式事件列表
     */
    public List<ChatStreamEvent> getStreamEventsByType(String conversationId, List<StreamEventType> types) {
        if (conversationId == null || conversationId.isEmpty() || types == null || types.isEmpty()) {
            return Collections.emptyList();
        }

        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                .and("type").in(types))
                .with(Sort.by(Sort.Direction.ASC, "sequence"));

        return mongoTemplate.find(query, ChatStreamEvent.class);
    }

    /**
     * 转换为StreamEvent列表
     *
     * @param chatStreamEvents ChatStreamEvent列表
     * @return StreamEvent列表
     */
    public List<StreamEvent> toStreamEvents(List<ChatStreamEvent> chatStreamEvents) {
        return chatStreamEvents.stream()
                .map(ChatStreamEvent::toStreamEvent)
                .collect(Collectors.toList());
    }

    /**
     * 删除会话的所有流式事件
     *
     * @param conversationId 会话ID
     */
    public void clearStreamEvents(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, ChatStreamEvent.class);
        log.info("删除会话流式事件: conversationId={}", conversationId);
    }

    /**
     * 获取用户的所有会话ID列表
     *
     * @param userId 用户ID
     * @return 会话ID列表
     */
    public List<String> findAllConversationIds(String userId) {
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建正则表达式：以 userId + "_" 开头
        Pattern pattern = Pattern.compile("^" + Pattern.quote(userId) + "_");
        Query query = new Query(Criteria.where("conversationId").regex(pattern));

        return mongoTemplate.findDistinct(
                query,
                "conversationId",
                ChatStreamEvent.class,
                String.class);
    }

    /**
     * 获取会话的事件数量
     *
     * @param conversationId 会话ID
     * @return 事件数量
     */
    private int getEventCountByConversationId(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        return (int) mongoTemplate.count(query, ChatStreamEvent.class);
    }
}