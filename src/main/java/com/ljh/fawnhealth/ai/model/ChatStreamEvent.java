package com.ljh.fawnhealth.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 聊天流式事件实体类，用于存储完整的交互历史
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chatStreamEvents")
public class ChatStreamEvent {

    @Id
    private ObjectId id;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 事件类型：THINKING、TOOL_RESPONSE、FINAL_RESPONSE等
     */
    private StreamEventType type;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 事件时间戳
     */
    private long timestamp;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 事件顺序号（同一会话内的顺序）
     */
    private Integer sequence;

    /**
     * 用户ID（可选，用于权限控制）
     */
    private String userId;

    /**
     * 从StreamEvent创建ChatStreamEvent
     */
    public static ChatStreamEvent fromStreamEvent(String conversationId, StreamEvent streamEvent, Integer sequence,
            String userId) {
        ChatStreamEvent chatStreamEvent = new ChatStreamEvent();
        chatStreamEvent.setConversationId(conversationId);
        chatStreamEvent.setType(streamEvent.getType());
        chatStreamEvent.setContent(streamEvent.getContent());
        chatStreamEvent.setTimestamp(streamEvent.getTimestamp());
        chatStreamEvent.setCreateTime(LocalDateTime.now());
        chatStreamEvent.setSequence(sequence);
        chatStreamEvent.setUserId(userId);
        return chatStreamEvent;
    }

    /**
     * 转换为StreamEvent
     */
    public StreamEvent toStreamEvent() {
        return new StreamEvent(this.type, this.content, this.timestamp);
    }
}