package com.zr.health.ai.store;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
//聊天消息序列化器
public class MessageSerializer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Message.class, new MessageDeserializer());
        objectMapper.registerModule(module);
    }

    public static String messagesToJson(List<Message> messages) throws JsonProcessingException {
        return objectMapper.writeValueAsString(messages);
    }

    public static List<Message> messagesFromJson(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
    }

    private static class MessageDeserializer extends JsonDeserializer<Message> {
        @Override
        public Message deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            Map<String, Object> node = p.readValueAs(Map.class);
            String type = (String) node.get("messageType");

            switch (type) {
                case "USER":
                    return new UserMessage((String) node.get("text"));
                case "ASSISTANT":
                    return new AssistantMessage((String) node.get("text"));
                default:
                    throw new IOException("未知消息类型: " + type);
            }
        }
    }
}