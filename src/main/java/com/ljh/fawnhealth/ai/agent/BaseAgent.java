package com.ljh.fawnhealth.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.ljh.fawnhealth.ai.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现 step 方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;       // 系统提示词
    private String nextStepPrompt;     // 下一步骤提示词

    // 代理状态
    private AgentState state = AgentState.IDLE;  // 当前状态，默认为闲置

    // 执行步骤控制
    private int currentStep = 0;       // 当前执行步数
    private int maxSteps = 10;         // 最大执行步数

    // LLM 大模型客户端
    private ChatClient chatClient;

    // 记忆存储（维护对话上下文）
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     *
     * @param userPrompt 用户输入的提示词
     * @return 执行结果字符串
     */
    public String run(String userPrompt) {
        // 1. 基础校验：确保代理处于空闲状态，并且用户提示词不为空
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("代理当前状态无法运行: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("用户提示词不能为空");
        }

        // 2. 开始运行，更改状态
        this.state = AgentState.RUNNING;
        messageList.add(new UserMessage(userPrompt));    // 添加用户消息到记忆中
        List<String> results = new ArrayList<>();        // 保存每一步的结果

        try {
            // 执行最大 maxSteps 步
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("正在执行第 {}/{} 步", stepNumber, maxSteps);

                String stepResult = step();  // 执行单个步骤
                String result = "第 " + stepNumber + " 步: " + stepResult;
                results.add(result);         // 保存结果
            }

            // 如果达到最大步数仍未完成
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("终止：已达到最大执行步数 (" + maxSteps + ")");
            }

            return String.join("\n", results);  // 返回所有结果拼接的字符串
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("执行代理时发生错误", e);
            return "执行错误：" + e.getMessage();
        } finally {
            this.cleanup();  // 清理资源
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户输入的提示词
     * @return SseEmitter 实例，用于前端实时接收数据
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter sseEmitter = new SseEmitter(300000L); // 设置超时时间为5分钟

        // 使用异步线程处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 基础校验
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
                return;
            }

            // 2. 开始运行，更改状态
            this.state = AgentState.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            List<String> results = new ArrayList<>();

            try {
                // 循环执行最多 maxSteps 步
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("正在执行第 {}/{} 步", stepNumber, maxSteps);
                    // 单步执行
                    String stepResult = step();
                    String result = "第 " + stepNumber + " 步: " + stepResult;
                    results.add(result);
                    // 将每一步结果发送给前端
                    sseEmitter.send(result);
                }

                // 判断是否超出最大步数限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    results.add("终止：已达到最大执行步数 (" + maxSteps + ")");
                    sseEmitter.send("执行结束：达到最大步骤（" + maxSteps + "）");
                }
                // 正常完成
                sseEmitter.complete();
            } catch (IOException e) {
                state = AgentState.ERROR;
                log.error("执行智能体发生错误", e);
                try {
                    sseEmitter.send("执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                this.cleanup();  // 清理资源
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE 连接超时");
        });

        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE 连接已完成");
        });

        return sseEmitter;
    }

    /**
     * 单个步骤的逻辑，由子类实现
     *
     * @return 当前步骤的执行结果
     */
    public abstract String step();

    /**
     * 清理资源的方法，子类可以重写此方法来释放资源
     */
    protected void cleanup() {
        // 默认为空，子类可自定义清理逻辑
    }
}