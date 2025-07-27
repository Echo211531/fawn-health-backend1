package com.ljh.fawnhealth.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ljh.fawnhealth.ai.agent.model.AgentState;
import com.ljh.fawnhealth.ai.agent.model.StreamEvent;
import com.ljh.fawnhealth.ai.agent.model.StreamEventType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

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
    protected String chatId;  // 统一会话ID字段

    public BaseAgent() {
        this(null); // 调用带参数的构造函数
    }

    public BaseAgent(String chatId) {
        this.chatId = chatId;
    }


    // 提示词
    private String systemPrompt;       // 系统提示词
    private String nextStepPrompt;     // 下一步骤提示词

    // 代理状态
    private AgentState state = AgentState.IDLE;  // 当前状态，默认为闲置
    private int currentStep = 0;       // 当前执行步数
    private int maxSteps = 5;         // 最大执行步数

    // LLM 大模型客户端
    protected ChatClient chatClient;

    // 标记最终响应发送状态
    private boolean finalResponseSent = false;

    // 记忆存储（维护对话上下文）
    protected List<Message> messageList = new ArrayList<>();


    // 添加线程池
    protected static final ExecutorService executor = Executors.newCachedThreadPool();


    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户输入的提示词
     * @return SseEmitter 实例，用于前端实时接收数据
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建SseEmitter，设置较长的超时时间
        SseEmitter sseEmitter = new SseEmitter(300000L); // 设置超时时间为5分钟
        // 重置状态标志
        finalResponseSent = false;

        // 使用异步线程处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1. 基础校验
                if (this.state != AgentState.IDLE) {
                    sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE, "错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE, "错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }

            // 2. 开始运行，更改状态
            this.state = AgentState.RUNNING;
            // 记录用户消息
            messageList.add(new UserMessage(userPrompt));

            try {
                // 循环执行最多 maxSteps 步
                for (int i = 0; i < maxSteps; i++) {
                    // 增加状态日志
                    log.debug("当前状态: {}, 步骤: {}/{}", state, currentStep, maxSteps);
                    // 增加状态检查点
                    if (state == AgentState.FINISHED || state == AgentState.ERROR) {
                        log.info("检测到终止状态，跳出循环");
                        break;
                    }
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("正在执行第 {}/{} 步", stepNumber, maxSteps);

                    // 执行流式步骤
                    streamStep(sseEmitter);
                    // +++ 添加最终响应检查 +++
                    if (finalResponseSent) {
                        log.info("最终总结已生成，终止执行");
                        break;
                    }
                    // +++ 检查状态是否为 FINISHED +++
                    if (state == AgentState.FINISHED) {
                        log.info("检测到终止状态，跳出循环");
                        break;
                    }
                }

                // 3. 生成最终响应（如果未发送）
                if (!finalResponseSent&& state == AgentState.FINISHED) {
                    // 使用对话历史生成总结
                    String conversationHistory = getConversationHistory();
                    String summaryPrompt = "请基于以下对话历史生成精简总结：\n\n" + conversationHistory;
                    List<Message> summaryMessages = new ArrayList<>();
                    summaryMessages.add(new SystemMessage("生成任务总结"));
                    summaryMessages.add(new UserMessage(summaryPrompt));

                    // 优化总结提示词，要求更精确的总结
                    summaryPrompt = "请基于以下完整的思考和工具执行过程，生成一个该任务的总结（不超过600字）：\n\n" +
                            "### 完整对话历史:\n" + conversationHistory;
                    summaryMessages.clear();
                    summaryMessages.add(new SystemMessage("你是一个专业总结助手，请用简洁的语言总结核心结论"));
                    summaryMessages.add(new UserMessage(summaryPrompt));

                    ChatResponse response = getChatClient().prompt(summaryPrompt)
                            .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                    .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                            .call()
                            .chatResponse();

                    if (response.getResult() != null &&
                            response.getResult().getOutput() != null) {

                        String summary = response.getResult().getOutput().getText();
                        streamFinalResponse(summary, sseEmitter);
                    } else {
                        sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE, "任务完成");
                    }
                    finalResponseSent = true;
                }
                // 正常完成
                sseEmitter.complete();

            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("执行智能体发生错误", e);
                try {
                    sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE, "执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (Exception ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                this.cleanup();  // 清理资源
            }
        }, executor);

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


    // 添加最终响应标记方法
    public void markFinalResponseSent() {
        this.finalResponseSent = true;
    }

    /**
     * 发送流式事件
     */
    protected void sendStreamEvent(SseEmitter emitter, StreamEventType type, String content) {
        try {
            // 过滤空内容
            if (StrUtil.isBlank(content)) {
                log.warn("尝试发送空内容的事件: {}", type);
                return;
            }
            emitter.send(new StreamEvent(type, content));
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
        }
    }

    /**
     * 流式输出最终响应 (基类实现)
     */
    protected void streamFinalResponse(String content, SseEmitter emitter) {
        // 添加空内容检查
        if (StrUtil.isBlank(content)) {
            log.warn("尝试发送空内容的最终响应");
            return;
        }
        // 清理冗余内容
        String cleanContent = content
                .replace("terminate()", "")
                .replace("TERMINATE", "")
                .replaceAll("(?i)总结[:：]", "") // 移除"总结："前缀
                .trim();
        // 添加总结标识
        String finalOutput = "📝 最终总结:\n" + cleanContent;
        // 单次发送完整响应（避免分块导致的重复）
        sendStreamEvent(emitter, StreamEventType.FINAL_RESPONSE, " AI最终结论：\n" + finalOutput);
    }


    /**
     * 流式步骤执行
     *
     * @param emitter SSE发射器
     */
    protected abstract void streamStep(SseEmitter emitter) throws Exception;

    /**
     * 单个步骤的逻辑，由子类实现
     *
     * @return 当前步骤的执行结果
     */
    public abstract String step();

    //获取对话历史文本
    protected String getConversationHistory() {
        return messageList.stream()
                .filter(msg -> msg.getText() != null)
                .map(msg -> {
                    String role = "";
                    if (msg instanceof UserMessage) role = "用户";
                    else if (msg instanceof AssistantMessage) role = "助手";
                    else if (msg instanceof SystemMessage) role = "系统";
                    return String.format("[%s]: %s", role, msg.getText());
                })
                .collect(Collectors.joining("\n"));
    }
    /**
     * 清理资源的方法，子类可以重写此方法来释放资源
     */
    protected void cleanup() {
        // 默认为空，子类可自定义清理逻辑
    }
}