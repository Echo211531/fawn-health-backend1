package com.zr.health.ai.tool;

import com.zr.health.ai.agent.queue.UserInputQueue;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户交互工具
 * 允许AI主动询问用户获取更多信息
 */
@Component
public class UserInteractionTool {

    @Autowired
    private UserInputQueue userInputQueue;

    @Tool(description = """
            当需要用户提供额外信息或确认时，调用此工具向用户提问。
            使用场景：
            1. 需要用户确认某个选择
            2. 需要用户提供更多详细信息
            3. 需要用户选择偏好
            4. 需要用户确认操作

            参数说明：
            - question: 要向用户提出的问题
            - timeout: 等待用户回答的超时时间（秒），默认30秒

            调用此工具后，系统会等待用户通过 /ai/user/input 接口提供回答。
            """)
    public String askUser(String question, Integer timeout) {
        try {
            if (timeout == null || timeout <= 0) {
                timeout = 30; // 默认30秒超时
            }

            // 记录问题
            System.out.println("🤔 AI询问用户: " + question);

            // 等待用户回答
            String userResponse = userInputQueue.takeResponse();

            if (userResponse != null && !userResponse.trim().isEmpty()) {
                return "用户回答: " + userResponse;
            } else {
                return "用户未提供回答或回答为空";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "等待用户回答时被中断";
        } catch (Exception e) {
            return "获取用户回答时出现错误: " + e.getMessage();
        }
    }

    @Tool(description = """
            向用户提出简单的是/否问题，等待用户确认。
            适用于需要用户快速确认的场景。
            """)
    public String askUserConfirmation(String question) {
        return askUser(question + " (请回答：是/否)", 30);
    }

    @Tool(description = """
            向用户询问选择，提供多个选项供用户选择。
            适用于需要用户从多个选项中选择的场景。
            """)
    public String askUserChoice(String question, String options) {
        String fullQuestion = question + "\n选项：" + options + "\n请选择对应的选项。";
        return askUser(fullQuestion, 60);
    }
}