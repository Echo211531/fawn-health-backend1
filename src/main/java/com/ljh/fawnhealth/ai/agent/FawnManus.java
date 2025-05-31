package com.ljh.fawnhealth.ai.agent;

import com.ljh.fawnhealth.ai.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

@Component
public class FawnManus extends ToolCallAgent {
    public FawnManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);  
        this.setName("fawnManus");
        String SYSTEM_PROMPT = """  
          你是一个全能的人工智能助手，你可以调用各种工具来完成用户给你的任务。
          接收到用户的任务，你需要根据用户提出的要求，分析出完成用户的步骤所需的流程，以第一步、第二步的方式来逐步完成用户的需求。
          为完成用户的需求，你可以调用提供的工具，但是需要在有正确的（非报错或没有找到）结果之后，能够进行下一步操作，减少重复调用工具的行为，以浪费系统资源。
          如果执行任务产生报错或者搜索失败，请务必再次尝试，可以切换搜索关键词，这有利于提升用户体验
          """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
          根据用户需求，主动选择最合适的工具或工具组合。对于复杂的任务，您可以分解问题并逐步使用不同的工具来解决它。
          使用每个工具后，清楚地解释执行结果并建议后续步骤。
          如果执行任务产生报错，请再次尝试，这有利于提升用户体验
          如果要在任何时候停止交互，请使用 'terminate' 工具调用，但是调用之前，确保完成任务的最后一步，不要没有完成任务提前退出
          """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);  
        // 初始化客户端  
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
            .defaultAdvisors(new MyLoggerAdvisor())
            .build();  
        this.setChatClient(chatClient);  
    }  
}