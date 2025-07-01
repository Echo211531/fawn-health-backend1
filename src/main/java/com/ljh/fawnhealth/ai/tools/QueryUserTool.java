package com.ljh.fawnhealth.ai.tools;

import com.ljh.fawnhealth.ai.agent.queue.UserInputQueue;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

//AI大模型向用户提问的工具
@Component
public class QueryUserTool {
    @Resource
    private UserInputQueue userInputQueue;

    /**
     * 工具方法：向用户提问，并等待用户输入
     */
    @Tool(description = "当需要用户补充信息时，向用户提问并等待其输入")
    public String askUserAndWait(
            @ToolParam(description = "需要用户提供额外信息的问题，例如：'请提供您的年龄' 或 '您希望选择哪个选项？A/B/C'")
            String question) {
        System.out.println("【系统提问】" + question);
        try {
            // 阻塞等待用户输入
            String userAnswer = userInputQueue.takeResponse();
            System.out.println("【用户回答】" + userAnswer);
            return userAnswer;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "用户取消了操作";
        }
    }
}