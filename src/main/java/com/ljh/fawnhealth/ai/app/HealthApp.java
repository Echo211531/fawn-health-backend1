package com.ljh.fawnhealth.ai.app;

import cn.hutool.core.util.StrUtil;
import com.ljh.fawnhealth.ai.advisor.MyLoggerAdvisor;
import com.ljh.fawnhealth.ai.advisor.ProhibitedWordAdvisor;
import com.ljh.fawnhealth.ai.agent.queue.UserInputQueue;
import com.ljh.fawnhealth.ai.rag.QueryRewriter;
import com.ljh.fawnhealth.ai.store.MongoChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class HealthApp {

    private final ChatClient chatClient;
    //初始化ChatClient
    public HealthApp(ChatModel dashscopeChatModel, MongoChatMemory mongoChatMemory) throws IOException {
        // 从resources文件夹下读取prompt.txt文件内容
        Resource resource = new ClassPathResource("prompt.txt");
        String systemPrompt = new String(Files.readAllBytes(Paths.get(resource.getURI())));
//        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(systemPrompt)  //系统预设
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(mongoChatMemory), //对话记忆
                        //自定义日志  Advisor，可按需开启
                        new MyLoggerAdvisor(),
                        // 自定义违禁词 Advisor，可按需开启
                        new ProhibitedWordAdvisor()
                        //自定义推理增强，可按需开启
                        //new ReReadingAdvisor()
                )
                .build();
    }

    //流式调用
    public Flux<String> doChat(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) //设置最大记忆数
                .stream()
                .content();
    }


    // 注入 QueryRewriter 和 VectorStore
    @Autowired
    private QueryRewriter queryRewriter;
    @Autowired
    private VectorStore healthAppVectorStore;
    @Autowired
    private Advisor healthAppRagCloudAdvisor;

    @jakarta.annotation.Resource
    private VectorStore pgVectorVectorStore;

    //支持检索增强会话
    public Flux<String> doChatWithRag(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        return chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // rag应用 （基于 PgVector 向量存储）
               .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用知识库问答 （基于内存）
                //.advisors(new QuestionAnswerAdvisor(healthAppVectorStore))
                // 应用增强检索服务（云知识库服务）
 //             .advisors(healthAppRagCloudAdvisor)
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        HealthAppRagCustomAdvisorFactory.createHealthAppRagCustomAdvisor(
//                                healthAppVectorStore, "file"
//                       )
//                )
                .stream()
                .content();
    }


    @jakarta.annotation.Resource
    private ToolCallbackProvider toolCallbackProvider;

    // Mcp 服务
    public Flux<String> doChatWithMcp(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .stream()
                .content();
    }


}       