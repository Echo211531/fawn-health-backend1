package com.ljh.fawnhealth.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.ljh.fawnhealth.ai.context.AgentContext;
import com.ljh.fawnhealth.ai.model.AgentState;
import com.ljh.fawnhealth.ai.model.StreamEvent;
import com.ljh.fawnhealth.ai.model.StreamEventType;
import com.ljh.fawnhealth.ai.store.ChatStreamEventStore;
import com.ljh.fawnhealth.ai.tool.collection.ToolCollection;
import com.ljh.fawnhealth.context.BaseContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private String name; // 代理名称
    private String systemPrompt; // 系统提示词
    private String nextStepPrompt; // 下一步骤提示词
    public ToolCallback[] availableTools; // 可用的工具
    public ToolCollection toolCollection = new ToolCollection(); // 工具集合
    protected List<Message> messageList = new ArrayList<>(); // 记忆存储（维护对话上下文）
    protected ChatClient chatClient; // LLM 大模型客户端
    protected AgentContext context; // 智能体上下文

    protected String chatId; // 统一会话ID字段

    // 添加流式事件存储器，使用静态方式注入避免循环依赖
    private static ChatStreamEventStore chatStreamEventStore;

    // 静态方法设置存储器
    public static void setChatStreamEventStore(ChatStreamEventStore store) {
        chatStreamEventStore = store;
    }

    public BaseAgent() {
        this(null); // 调用带参数的构造函数
    }

    public BaseAgent(String chatId) {
        this.chatId = chatId;
    }

    // 执行控制
    private AgentState state = AgentState.IDLE; // 当前状态，默认为闲置
    private int currentStep = 0; // 当前执行步数
    private int maxSteps = 5; // 最大执行步数

    // 标记最终响应发送状态
    private boolean finalResponseSent = false;
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
            // 保存用户消息到流式事件存储，确保上下文记录一致
            if (chatStreamEventStore != null && StrUtil.isNotBlank(chatId)) {
                try {
                    String userId = getCurrentUserId();
                    StreamEvent userMessageEvent = new StreamEvent(StreamEventType.USER_MESSAGE, userPrompt);
                    chatStreamEventStore.saveStreamEvent(chatId, userMessageEvent, userId);
                    log.debug("保存用户消息到流式事件存储: chatId={}, message={}", chatId, userPrompt);
                } catch (Exception e) {
                    log.error("保存用户消息到流式事件存储失败: chatId={}", chatId, e);
                }
            }

            try {
                // 循环执行最多 maxSteps 步
                for (int i = 0; i < maxSteps; i++) {
                    // 增加状态日志
                    log.debug("当前状态: {}, 步骤: {}/{}", state, currentStep, maxSteps);
                    // 增加状态检查点
                    if (state == AgentState.FINISHED || state == AgentState.ERROR) {
                        log.info("🔚 检测到终止状态，跳出循环");
                        break;
                    }
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("🔄 正在执行第 {}/{} 步", stepNumber, maxSteps);

                    // 执行流式步骤
                    streamStep(sseEmitter);

                    // 检查是否应该终止
                    if (finalResponseSent) {
                        log.info("✅ 最终总结已生成，终止执行");
                        break;
                    }
                    if (state == AgentState.FINISHED) {
                        log.info("🔚 检测到终止状态，跳出循环");
                        break;
                    }

                    // 如果达到最大步数，强制终止
                    if (stepNumber >= maxSteps) {
                        log.info("⏰ 达到最大步数限制，强制终止");
                        setState(AgentState.FINISHED);
                        break;
                    }

                    // 添加延迟，避免过快循环
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 3. 生成最终响应（如果未发送）
                if (!finalResponseSent) {
                    // 使用对话历史生成总结
                    String conversationHistory = getConversationHistory();

                    // 优化总结提示词，要求更精确的总结
                    String summaryPrompt = """
                            请基于以下完整的思考和工具执行过程，生成一个该任务的最终总结：

                            ### 任务要求：
                            - 总结要简洁明了，突出重点
                            - 包含关键信息和结论
                            - 不超过2000字
                            - 使用中文回答
                            - 如果涉及工具搜索结果，请整合相关信息给出完整答案

                            ### 完整对话历史:
                            """ + conversationHistory;

                    List<Message> summaryMessages = new ArrayList<>();
                    summaryMessages.add(new SystemMessage("你是一个专业的总结助手，请基于完整的对话历史和工具执行结果，总结一下上下文的核心内容，不超过2000字"));
                    summaryMessages.add(new UserMessage(summaryPrompt));

                    try {
                        log.info("🤖 开始基于完整上下文生成最终总结...");
                        ChatResponse response = getChatClient().prompt(summaryPrompt)
                                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                                .call()
                                .chatResponse();

                        if (response.getResult() != null &&
                                response.getResult().getOutput() != null) {

                            String summary = response.getResult().getOutput().getText();
                            log.info("✅ AI生成最终总结完成，长度: {}", summary.length());
                            streamFinalResponse(summary, sseEmitter);
                        } else {
                            log.warn("AI返回的总结为空");
                            sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE, "✅ 任务完成");
                        }
                    } catch (Exception e) {
                        log.error("生成最终总结失败", e);
                        sendStreamEvent(sseEmitter, StreamEventType.FINAL_RESPONSE,
                                "✅ 任务完成，但生成总结时出现错误: " + e.getMessage());
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
                this.cleanup(); // 清理资源
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
            
            StreamEvent streamEvent = new StreamEvent(type, content);
            emitter.send(streamEvent);
            
            // 保存流式事件到数据库
            if (chatStreamEventStore != null && StrUtil.isNotBlank(chatId)) {
                try {
                    // 获取当前用户ID
                    String userId = getCurrentUserId();
                    chatStreamEventStore.saveStreamEvent(chatId, streamEvent, userId);
                } catch (Exception e) {
                    log.error("保存流式事件到数据库失败: chatId={}, type={}", chatId, type, e);
                }
            }
        } catch (IOException e) {
            log.error("发送SSE事件失败", e);
        }
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Long currentId = BaseContext.getCurrentId();
            return currentId != null ? currentId.toString() : "unknown";
        } catch (Exception e) {
            log.debug("获取当前用户ID失败，使用默认值", e);
            return "unknown";
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
                .replaceAll("(?i)结论[:：]", "") // 移除"结论："前缀
                .replaceAll("(?i)答案[:：]", "") // 移除"答案："前缀
                .trim();

        // 格式化最终输出
        String finalOutput = """
                🎯 **最终答案**

                %s

                ---
                *任务执行完成*
                """.formatted(cleanContent);

        // 单次发送完整响应（避免分块导致的重复）
        sendStreamEvent(emitter, StreamEventType.FINAL_RESPONSE, finalOutput);
    }

    /**
     * 流式步骤执行
     *
     * @param emitter SSE发射器
     */
    protected abstract void streamStep(SseEmitter emitter) throws Exception;

    // 获取对话历史文本
    protected String getConversationHistory() {
        return messageList.stream()
                .filter(msg -> msg.getText() != null)
                .map(msg -> {
                    String role = "";
                    if (msg instanceof UserMessage)
                        role = "用户";
                    else if (msg instanceof AssistantMessage)
                        role = "助手";
                    else if (msg instanceof SystemMessage)
                        role = "系统";
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