package com.ljh.fawnhealth.ai.agent;

import com.ljh.fawnhealth.ai.advisor.MyLoggerAdvisor;
import com.ljh.fawnhealth.ai.advisor.ProhibitedWordAdvisor;
import com.ljh.fawnhealth.ai.advisor.ReReadingAdvisor;
import com.ljh.fawnhealth.ai.context.AgentContext;
import com.ljh.fawnhealth.ai.prompt.ToolCallPrompt;
import com.ljh.fawnhealth.ai.store.MongoChatMemory;
import com.ljh.fawnhealth.ai.tool.collection.ToolCollection;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
