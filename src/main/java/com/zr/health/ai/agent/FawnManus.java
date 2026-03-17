package com.zr.health.ai.agent;

import com.zr.health.ai.advisor.MyLoggerAdvisor;
import com.zr.health.ai.context.AgentContext;
import com.zr.health.ai.store.MongoChatMemory;
import com.zr.health.ai.tool.collection.ToolCollection;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FawnManus extends ToolCallAgent {
        private final MongoChatMemory mongoChatMemory;

        /**
         * 使用AgentContext构造
         */
        public FawnManus(AgentContext context, ChatModel dashscopeChatModel,
                        MongoChatMemory mongoChatMemory) {
                super(context);
                this.mongoChatMemory = mongoChatMemory;
                init(dashscopeChatModel);
        }

        private void init(ChatModel dashscopeChatModel) {
                // 设置智能体名称为"小鹿"，这会在ToolCallAgent的initPromptsWithContext中被使用
                this.setName("小鹿");
                this.setMaxSteps(10); // 增加最大步数，支持更复杂的任务
                // 初始化超级智能体客户端
                ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                                .defaultAdvisors(
                                                new MessageChatMemoryAdvisor(mongoChatMemory), // 对话记忆
                                                new MyLoggerAdvisor()// 自定义日志
                                // 自定义违禁词 Advisor，可按需开启
                                // new ProhibitedWordAdvisor()
                                // 自定义推理增强，可按需开启
                                // new ReReadingAdvisor()
                                )
                                .build();
                this.setChatClient(chatClient);
        }

        /**
         * 创建AgentContext
         */
        public static AgentContext createContext(String query, String chatId, ToolCollection toolCollection) {
                String requestId = UUID.randomUUID().toString();
                String dateInfo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return AgentContext.builder()
                                .requestId(requestId)
                                .chatId(chatId)
                                .query(query)
                                .toolCollection(toolCollection)
                                .dateInfo(dateInfo)
                                .isStream(true)
                                .build();
        }
}
