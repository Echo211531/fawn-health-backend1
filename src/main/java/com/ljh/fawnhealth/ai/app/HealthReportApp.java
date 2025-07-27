package com.ljh.fawnhealth.ai.app;

import com.ljh.fawnhealth.ai.advisor.MyLoggerAdvisor;
import com.ljh.fawnhealth.ai.advisor.ProhibitedWordAdvisor;
import com.ljh.fawnhealth.ai.store.MongoChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;
//健康报告对话
@Component
@Slf4j
public class HealthReportApp {

    private final ChatClient chatClient1;
    private static final String SYSTEM_PROMPT = "你的名字是“小鹿”，你是一家名为“北京协和医院”的智能客服。你是一个训练有素的医疗顾问和医疗伴诊助手。你态度友好、礼貌且言辞简洁。\n" +
            "1、请仅在用户发起第一次会话时，和用户打个招呼，并介绍你是谁。\n" ;

    //初始化ChatClient
    public HealthReportApp(ChatModel dashscopeChatModel,MongoChatMemory mongoChatMemory) throws IOException {
        chatClient1 = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)  //系统预设
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(mongoChatMemory), //对话记忆
                        //自定义日志  Advisor，可按需开启
                        new MyLoggerAdvisor()
                        // 自定义违禁词 Advisor，可按需开启
                        //new ProhibitedWordAdvisor()
                        //自定义推理增强，可按需开启
                        //new ReReadingAdvisor()
                )
                .build();
    }

    public record HealthReport(String title, List<String> suggestions) { }

    //生成健康报告对话
    public HealthReport doChatWithReport(String message, String chatId) {
        HealthReport healthReport = chatClient1
                .prompt()
                .system(SYSTEM_PROMPT + "分析用户提供的信息，每次对话后都要生成健康报告，标题为{用户名}的健康报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(HealthReport.class);
        log.info("healthReport: {}", healthReport);
        return  healthReport;
    }

}       