package com.zr.health.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.zr.health.ai.context.AgentContext;
import com.zr.health.ai.model.AgentState;
import com.zr.health.ai.model.StreamEventType;
import com.zr.health.ai.prompt.ToolCallPrompt;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {
    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;
    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;
    // 系统提示词快照
    private String systemPromptSnapshot;
    // 下一步提示词快照
    private String nextStepPromptSnapshot;
    // 工具执行计划：存储需要执行的所有工具
    private List<AssistantMessage.ToolCall> toolExecutionPlan = new ArrayList<>();
    // 当前执行的工具索引
    private int currentToolIndex = 0;
    // 是否已完成工具规划
    private boolean toolPlanningCompleted = false;

    // 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    // 使用AgentContext构造
    public ToolCallAgent(AgentContext context) {
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
        this.setChatId(context.getChatId());
        this.context = context;

        // 初始化工具集合
        if (context.getToolCollection() != null) {
            this.toolCollection = context.getToolCollection();
            // 初始化availableTools
            this.availableTools = this.toolCollection.getAllTools();
            log.info("ToolCallAgent初始化完成，工具数量: {}",
                    this.availableTools != null ? this.availableTools.length : 0);
        } else {
            log.warn("AgentContext中的toolCollection为null");
        }

        // 初始化提示词
        initPromptsWithContext(context);
    }

    // 使用上下文初始化提示词
    private void initPromptsWithContext(AgentContext context) {
        StringBuilder toolPrompt = new StringBuilder();
        // 安全处理工具集合，避免null值
        if (toolCollection != null && toolCollection.getAllTools() != null) {
            for (ToolCallback tool : toolCollection.getAllTools()) {
                if (tool != null) {
                    toolPrompt.append(String.format("工具名：%s 工具描述：%s\n", tool.getName(), tool.getDescription()));
                }
            }
        }

        // 获取智能体名称，如果没有设置则使用默认值
        String agentName = getName();
        String agentNamePlaceholder = StrUtil.isNotBlank(agentName) ? "，名叫" + agentName : "";

        // 安全获取上下文值，避免null值
        String query = StrUtil.isNotBlank(context.getQuery()) ? context.getQuery() : "";
        String dateInfo = StrUtil.isNotBlank(context.getDateInfo()) ? context.getDateInfo() : "";
        String basePrompt = StrUtil.isNotBlank(context.getBasePrompt()) ? context.getBasePrompt() : "";

        String systemPrompt = ToolCallPrompt.SYSTEM_PROMPT
                .replace("{{agentName}}", agentNamePlaceholder)
                .replace("{{tools}}", toolPrompt.toString())
                .replace("{{query}}", query)
                .replace("{{date}}", dateInfo)
                .replace("{{basePrompt}}", basePrompt);

        String nextStepPrompt = ToolCallPrompt.NEXT_STEP_PROMPT
                .replace("{{tools}}", toolPrompt.toString())
                .replace("{{query}}", query)
                .replace("{{date}}", dateInfo)
                .replace("{{basePrompt}}", basePrompt);

        setSystemPrompt(systemPrompt);
        setNextStepPrompt(nextStepPrompt);
        setSystemPromptSnapshot(systemPrompt);
        setNextStepPromptSnapshot(nextStepPrompt);
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think(SseEmitter emitter) {
        // 如果还没有完成工具规划，先进行工具规划
        if (!toolPlanningCompleted) {
            return planTools(emitter);
        }

        // 如果工具规划已完成，检查是否还有工具需要执行
        if (currentToolIndex < toolExecutionPlan.size()) {
            sendStreamEvent(emitter, StreamEventType.THINKING,
                    String.format("🔄 准备执行第 %d/%d 个工具", currentToolIndex + 1, toolExecutionPlan.size()));
            return true; // 需要执行工具
        }

        // 所有工具都已执行完成，生成最终答案
        sendStreamEvent(emitter, StreamEventType.THINKING, "🎯 所有工具执行完成，准备生成最终答案");
        setState(AgentState.FINISHED);
        return false; // 不需要继续执行工具
    }

    /**
     * 工具规划阶段：AI分析用户问题，确定需要调用哪些工具
     */
    private boolean planTools(SseEmitter emitter) {
        sendStreamEvent(emitter, StreamEventType.THINKING, "🤔 开始分析用户问题，规划需要调用的工具...");

        // 构建工具规划提示词
        String planningPrompt = buildToolPlanningPrompt();
        getMessageList().add(new UserMessage(planningPrompt));

        // 构造 Prompt
        Prompt prompt = new Prompt(messageList, chatOptions);

        // 调用 LLM 模型进行工具规划
        var requestSpec = getChatClient().prompt(prompt)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .system(getSystemPrompt());

        // 添加可用工具
        if (availableTools != null && availableTools.length > 0) {
            requestSpec = requestSpec.tools(availableTools);
        }

        Flux<ChatResponse> flux = requestSpec.stream().chatResponse();

        // 处理流式响应
        StringBuilder fullResponse = new StringBuilder();
        List<AssistantMessage.ToolCall> collectedToolCalls = new ArrayList<>();
        AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();
        CompletableFuture<Boolean> planningFuture = new CompletableFuture<>();

        flux.subscribe(
                chatResponse -> {
                    lastResponseRef.set(chatResponse);
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    if (output != null && output.getText() != null) {
                        String delta = output.getText();
                        if (StrUtil.isNotBlank(delta)) {
                            fullResponse.append(delta);
                        }
                    }
                    if (output != null && output.getToolCalls() != null) {
                        collectedToolCalls.addAll(output.getToolCalls());
                    }
                },
                error -> {
                    log.error("工具规划过程出错", error);
                    sendStreamEvent(emitter, StreamEventType.THINKING, "❌ 工具规划过程中断: " + error.getMessage());
                    planningFuture.completeExceptionally(error);
                },
                () -> {
                    ChatResponse lastResponse = lastResponseRef.get();
                    if (lastResponse == null) {
                        planningFuture.complete(false);
                        return;
                    }

                    AssistantMessage assistantMessage = lastResponse.getResult().getOutput();
                    List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();

                    // 发送规划思考内容
                    if (fullResponse.length() > 0) {
                        String thinkingContent = "💭 **工具规划分析**:\n" + fullResponse.toString();
                        sendStreamEvent(emitter, StreamEventType.THINKING, thinkingContent);
                        log.info("🤔 AI工具规划内容: {}", fullResponse.toString());
                    }

                    // 处理工具规划结果
                    if (toolCallList.isEmpty()) {
                        // 没有工具调用，直接生成最终答案
                        getMessageList().add(assistantMessage);
                        String content = assistantMessage.getText();
                        if (StrUtil.isNotBlank(content)) {
                            generateFinalSummary(emitter, content);
                            setState(AgentState.FINISHED);
                            markFinalResponseSent();
                        }
                        planningFuture.complete(false);
                    } else {
                        // 保存工具执行计划
                        toolExecutionPlan.clear();
                        toolExecutionPlan.addAll(toolCallList);
                        toolPlanningCompleted = true;
                        currentToolIndex = 0;

                        // 保存原始的ChatResponse，用于后续工具执行
                        this.toolCallChatResponse = lastResponse;

                        log.info("📋 工具规划完成，共规划 {} 个工具", toolCallList.size());

                        // 记录工具规划信息
                        String toolPlanInfo = toolCallList.stream()
                                .map(toolCall -> String.format("工具名称：%s，参数：%s",
                                        toolCall.name(),
                                        toolCall.arguments()))
                                .collect(Collectors.joining("\n"));

                        sendStreamEvent(emitter, StreamEventType.THINKING,
                                "📋 **工具执行计划**:\n" + toolPlanInfo);

                        planningFuture.complete(true); // 需要执行工具
                    }
                });

        try {
            return planningFuture.get(180, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("等待工具规划结果超时或出错", e);
            return false;
        }
    }

    /**
     * 构建工具规划提示词
     */
    private String buildToolPlanningPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请仔细分析用户的问题，确定需要调用哪些工具来完成任务。\n\n");
        prompt.append("### 分析要求：\n");
        prompt.append("1. 仔细理解用户问题的核心需求\n");
        prompt.append("2. 分析需要哪些工具来获取相关信息\n");
        prompt.append("3. 确定工具调用的顺序（如果涉及多个工具）\n");
        prompt.append("4. 每个工具只能调用一次\n");
        prompt.append("5. 如果不需要工具，请直接给出答案\n");
        prompt.append("6. 如果任务完成，可以使用doTerminate工具结束任务\n\n");
        prompt.append("### 可用工具：\n");

        if (availableTools != null) {
            for (ToolCallback tool : availableTools) {
                if (tool != null) {
                    prompt.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
                }
            }
        }

        prompt.append("\n### 用户问题：\n");
        prompt.append(context.getQuery());
        prompt.append("\n\n请分析并调用必要的工具。如果任务完成，请使用doTerminate工具结束。");

        return prompt.toString();
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act(SseEmitter emitter) {
        // 检查是否还有工具需要执行
        if (currentToolIndex >= toolExecutionPlan.size()) {
            return "所有工具已执行完成";
        }

        // 获取当前要执行的工具
        AssistantMessage.ToolCall currentToolCall = toolExecutionPlan.get(currentToolIndex);

        sendStreamEvent(emitter, StreamEventType.TOOL_RESPONSE,
                String.format("🔄 正在执行第 %d/%d 个工具: %s",
                        currentToolIndex + 1, toolExecutionPlan.size(), currentToolCall.name()));

        // 检查toolCallChatResponse是否为null
        if (this.toolCallChatResponse == null) {
            log.error("toolCallChatResponse为null，无法执行工具");
            String errorResult = "工具执行失败：toolCallChatResponse为null";
            sendStreamEvent(emitter, StreamEventType.TOOL_RESPONSE, errorResult);
            currentToolIndex++;
            return errorResult;
        }

        log.info("执行工具: {} - {}", currentToolCall.name(), currentToolCall.arguments());

        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = null;
        Map<String, String> uniqueResults = new LinkedHashMap<>();

        try {
            // 这里需要创建一个包含单个工具调用的响应
            // 暂时使用现有的逻辑，后续可以优化
            toolExecutionResult = toolCallingManager.executeToolCalls(
                    prompt, this.toolCallChatResponse);

            // 检查工具执行结果
            if (toolExecutionResult == null || toolExecutionResult.conversationHistory() == null ||
                    toolExecutionResult.conversationHistory().isEmpty()) {
                log.error("工具执行失败：toolExecutionResult为空或conversationHistory为空");
                uniqueResults.put(currentToolCall.name(), "工具执行失败：未获取到执行结果");
            } else {
                // 记录消息上下文
                setMessageList(toolExecutionResult.conversationHistory());

                // 当前工具调用的结果
                ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil
                        .getLast(toolExecutionResult.conversationHistory());

                if (toolResponseMessage == null) {
                    log.error("工具执行失败：toolResponseMessage为空");
                    uniqueResults.put(currentToolCall.name(), "工具执行失败：未获取到响应消息");
                } else {
                    // 生成工具响应（去重处理）
                    for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                        String toolName = response.name();
                        String responseData = response.responseData();

                        // 去重：如果相同工具的结果已存在
                        uniqueResults.put(toolName, responseData);
                    }
                }
            }
        } catch (Exception e) {
            log.error("工具执行过程中发生异常: {}", e.getMessage(), e);
            uniqueResults.put(currentToolCall.name(), "工具执行失败：" + e.getMessage());
        }

        // 构建工具响应字符串（格式化输出）
        StringBuilder resultsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : uniqueResults.entrySet()) {
            String toolName = entry.getKey();
            String result = entry.getValue();

            resultsBuilder.append("🛠️ 工具 [")
                    .append(toolName)
                    .append("] 执行结果：\n");

            // 检查是否为错误结果
            if (result != null && (result.contains("失败") || result.contains("错误"))) {
                resultsBuilder.append("❌ ").append(result);
            } else {
                resultsBuilder.append(result != null ? result : "未返回结果");
            }
            resultsBuilder.append("\n\n");
        }
        String results = resultsBuilder.toString().trim();

        // 添加空结果检查
        if (StrUtil.isBlank(results)) {
            results = "工具执行完成，但未返回有效结果";
        }

        // 发送工具响应事件
        sendStreamEvent(emitter, StreamEventType.TOOL_RESPONSE, results);

        // 检查是否已有相同工具的结果
        boolean toolResultExists = false;
        for (String toolName : uniqueResults.keySet()) {
            toolResultExists = getMessageList().stream()
                    .filter(msg -> msg instanceof UserMessage)
                    .anyMatch(msg -> msg.getText() != null &&
                            msg.getText().contains("工具 [" + toolName + "]"));
            if (toolResultExists) {
                break;
            }
        }

        // 避免重复添加相同工具的结果
        if (StrUtil.isNotBlank(results) && !toolResultExists) {
            getMessageList().add(new UserMessage(results));
        }

        // 分析工具执行结果
        analyzeToolResults(emitter, uniqueResults);

        // 判断是否调用了终止工具
        boolean terminateToolCalled = false;
        if (toolExecutionResult != null && CollUtil.isNotEmpty(toolExecutionResult.conversationHistory())) {
            Message lastMessage = CollUtil.getLast(toolExecutionResult.conversationHistory());
            if (lastMessage instanceof ToolResponseMessage) {
                ToolResponseMessage toolResponse = (ToolResponseMessage) lastMessage;
                for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                    if ("doTerminate".equals(response.name())) {
                        terminateToolCalled = true;
                        log.info("🔚 检测到doTerminate工具调用，准备终止任务");
                        break; // 找到终止工具后立即跳出循环
                    }
                }
            }
        }

        // 检查工具执行结果是否包含有效信息
        boolean hasValidInfo = results.contains("搜索结果") || results.contains("找到") ||
                results.contains("获取到") || results.contains("成功") ||
                results.length() > 100; // 有足够的内容

        // 如果调用了终止工具，立即结束任务
        if (terminateToolCalled) {
            log.info("🔚 检测到终止工具调用，设置状态为FINISHED");
            setState(AgentState.FINISHED);
            return results;
        }

        // 工具执行完成后，移动到下一个工具
        currentToolIndex++;

        if (currentToolIndex >= toolExecutionPlan.size()) {
            // 所有工具都已执行完成
            log.info("✅ 所有工具执行完成，共执行了 {} 个工具", toolExecutionPlan.size());
            setState(AgentState.FINISHED);
        } else {
            // 还有工具需要执行
            log.info("🔄 工具执行完成，准备执行下一个工具 ({}/{})",
                    currentToolIndex + 1, toolExecutionPlan.size());
        }

        return results;
    }

    /**
     * 生成最终总结
     */
    private void generateFinalSummary(SseEmitter emitter, String content) {
        try {
            // 清理内容
            String cleanContent = content
                    .replace("terminate()", "")
                    .replace("TERMINATE", "")
                    .replaceAll("(?i)总结[:：]", "")
                    .replaceAll("(?i)结论[:：]", "")
                    .replaceAll("(?i)答案[:：]", "")
                    .trim();

            // 格式化最终总结
            String finalSummary = """
                    🎯 **最终答案**

                    %s

                    ---
                    *任务执行完成*
                    """.formatted(cleanContent);

            sendStreamEvent(emitter, StreamEventType.FINAL_RESPONSE, finalSummary);
            log.info("✅ 生成最终总结完成: {}", cleanContent.substring(0, Math.min(100, cleanContent.length())));
        } catch (Exception e) {
            log.error("生成最终总结失败", e);
            sendStreamEvent(emitter, StreamEventType.FINAL_RESPONSE, "生成总结时出现错误: " + e.getMessage());
        }
    }

    /**
     * 检查是否已经执行过相同的工具调用
     */
    private boolean checkIfSameToolExecuted(List<AssistantMessage.ToolCall> currentToolCalls) {
        if (currentToolCalls == null || currentToolCalls.isEmpty()) {
            return false;
        }
        // 检查消息历史中是否已经执行过相同的工具调用
        for (Message message : getMessageList()) {
            if (message instanceof AssistantMessage) {
                AssistantMessage assistantMessage = (AssistantMessage) message;
                List<AssistantMessage.ToolCall> historicalToolCalls = assistantMessage.getToolCalls();

                if (historicalToolCalls != null && !historicalToolCalls.isEmpty()) {
                    // 比较当前工具调用和历史工具调用
                    for (AssistantMessage.ToolCall currentCall : currentToolCalls) {
                        for (AssistantMessage.ToolCall historicalCall : historicalToolCalls) {
                            if (currentCall.name().equals(historicalCall.name()) &&
                                    currentCall.arguments().equals(historicalCall.arguments())) {
                                log.info("🔄 发现重复的工具调用: {} - {}", currentCall.name(), currentCall.arguments());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 分析工具执行结果
     */
    private void analyzeToolResults(SseEmitter emitter, Map<String, String> toolResults) {
        try {
            StringBuilder analysis = new StringBuilder();
            analysis.append("🔍 **工具结果分析**:\n");

            boolean hasSuccess = false;
            boolean hasFailure = false;

            for (Map.Entry<String, String> entry : toolResults.entrySet()) {
                String toolName = entry.getKey();
                String result = entry.getValue();

                // 分析工具执行结果
                if (StrUtil.isBlank(result) || result.contains("错误") || result.contains("失败")) {
                    analysis.append("❌ ").append(toolName).append(": 执行失败或返回空结果\n");
                    hasFailure = true;
                } else if (result.length() > 200) {
                    analysis.append("✅ ").append(toolName).append(": 执行成功，返回大量数据\n");
                    hasSuccess = true;
                } else {
                    analysis.append("✅ ").append(toolName).append(": 执行成功\n");
                    hasSuccess = true;
                }
            }

            analysis.append("\n💡 **建议**: ");
            if (hasSuccess && !hasFailure) {
                analysis.append("所有工具执行成功，可以继续下一步操作");
            } else if (hasSuccess && hasFailure) {
                analysis.append("部分工具执行成功，部分工具执行失败，建议检查失败的工具");
            } else {
                analysis.append("工具执行可能存在问题，建议重新尝试或使用其他方法");
            }

            sendStreamEvent(emitter, StreamEventType.THINKING, analysis.toString());
            log.info("工具结果分析完成");
        } catch (Exception e) {
            log.error("分析工具结果失败", e);
            sendStreamEvent(emitter, StreamEventType.THINKING, "❌ 工具结果分析失败: " + e.getMessage());
        }
    }

}
