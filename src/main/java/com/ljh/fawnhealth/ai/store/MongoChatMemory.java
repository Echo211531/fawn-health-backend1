package com.ljh.fawnhealth.ai.store;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ljh.fawnhealth.ai.model.ChatMessages;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoChatMemory implements ChatMemory {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public void add(String conversationId, List<Message> messages) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);

        List<Message> updatedMessages;
        if (chatMessages != null) {
            try {
                updatedMessages = new java.util.ArrayList<>(chatMessages.getMessagesJson() != null
                        ? MessageSerializer.messagesFromJson(chatMessages.getMessagesJson()) : Collections.emptyList());
            } catch (JsonProcessingException e) {
                throw new RuntimeException("序列化消息失败", e);
            }
            updatedMessages.addAll(messages);
        } else {
            updatedMessages = new java.util.ArrayList<>(messages);
        }

        try {
            String json = MessageSerializer.messagesToJson(updatedMessages);
            if (chatMessages != null) {
                Update update = new Update().set("messagesJson", json);
                mongoTemplate.updateFirst(query, update, ChatMessages.class);
            } else {
                ChatMessages newChatMessages = new ChatMessages();
                newChatMessages.setConversationId(conversationId);
                newChatMessages.setMessagesJson(json);
                mongoTemplate.insert(newChatMessages);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化消息失败", e);
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);

        if (chatMessages == null || chatMessages.getMessagesJson() == null) {
            return Collections.emptyList();
        }

        try {
            List<Message> allMessages = MessageSerializer.messagesFromJson(chatMessages.getMessagesJson());
            int size = allMessages.size();
            int fromIndex = Math.max(0, size - lastN);
            return allMessages.subList(fromIndex, size);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("反序列化消息失败", e);
        }
    }

    @Override
    public void clear(String conversationId) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        mongoTemplate.remove(query, ChatMessages.class);
    }
}