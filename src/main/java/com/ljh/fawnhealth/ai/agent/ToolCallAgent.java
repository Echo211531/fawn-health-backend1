package com.ljh.fawnhealth.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.ljh.fawnhealth.ai.agent.model.AgentState;
import com.ljh.fawnhealth.ai.agent.model.StreamEventType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
    // 可用的工具
    private final ToolCallback[] availableTools;
    // 保存了工具调用信息的响应
    private ChatResponse toolCallChatResponse;
    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;
    public ToolCallAgent(ToolCallback[] availableTools, String chatId) {
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
        this.setChatId(chatId);
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think(SseEmitter emitter) {
        sendStreamEvent(emitter, StreamEventType.THINKING, "开始分析问题...");

        //1. 构建用户消息（可选）: 可以理解为 AI 主动提问或引导用户输入。
        String nextStepPrompt = getNextStepPrompt();
        if (StrUtil.isNotBlank(nextStepPrompt)) {
            getMessageList().add(new UserMessage(nextStepPrompt));
        }
        // 2.  获取消息列表，构造 Prompt（提示词）
        Prompt prompt = new Prompt(messageList, chatOptions);

        //3. 调用 LLM 模型进行思考,流式调用
        // 传入系统提示词、可用工具列表（tools）等信息
        Flux<ChatResponse> flux = getChatClient().prompt(prompt)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .system(getSystemPrompt())
                .tools(availableTools)
                .stream()
                .chatResponse();

        // 4. 处理流式响应
        StringBuilder fullResponse = new StringBuilder();

        List<AssistantMessage.ToolCall> collectedToolCalls = new ArrayList<>();
        AtomicReference<ChatResponse> lastResponseRef = new AtomicReference<>();
        // 创建 CompletableFuture 用于异步等待
        CompletableFuture<Boolean> thinkFuture = new CompletableFuture<>();
        flux.subscribe(
                chatResponse -> {
                    lastResponseRef.set(chatResponse); // 保存最后一个响应
                    // 提取出返回结果信息
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    if (output != null && output.getText() != null) {
                        String delta = output.getText();
                        // 过滤空内容
                        if (StrUtil.isNotBlank(delta)) {
                            fullResponse.append(delta);  //拼接结果信息
                        }
                    }
                    // 2. 收集工具调用增量信息
                    if (output != null && output.getToolCalls() != null) {
                        collectedToolCalls.addAll(output.getToolCalls());
                    }
                },
                error -> {
                    log.error("流式思考过程出错", error);
                    sendStreamEvent(emitter, StreamEventType.THINKING, "思考过程中断: " + error.getMessage());
                    thinkFuture.completeExceptionally(error);
                },
                () -> {
                    ChatResponse lastResponse = lastResponseRef.get();
                    if (lastResponse == null) {
                        thinkFuture.complete(false);
                        return;
                    }
                    // 使用原始响应中的结构化数据
                    AssistantMessage assistantMessage =
                            lastResponse.getResult().getOutput();
                    List<AssistantMessage.ToolCall> toolCallList =
                            assistantMessage.getToolCalls();
                    //5. 发送完整的思考内容
                    if (fullResponse.length() > 0) {
                        sendStreamEvent(emitter, StreamEventType.THINKING, fullResponse.toString());
                    }
                    //6. 判断是否需要调用工具
                    if (toolCallList.isEmpty()) {
                        // 没有工具调用，记录助手消息
                        getMessageList().add(assistantMessage);
                        // +++ 仅设置状态，不在这里生成响应 +++
                        setState(AgentState.FINISHED);
                        // +++ 直接作为最终响应发送 +++
                        String content = assistantMessage.getText();
                        if (StrUtil.isNotBlank(content)) {
                            streamFinalResponse(content, emitter);
                            markFinalResponseSent();  // 标记已发送
                        }
                        thinkFuture.complete(false);
                    } else {
                        this.toolCallChatResponse = lastResponse; // 使用原始响应

                        log.info("选择了 {} 个工具", toolCallList.size());

                        // 记录工具调用信息
                        String toolCallInfo = toolCallList.stream()
                                .map(toolCall -> String.format("工具名称：%s，参数：%s",
                                        toolCall.name(),
                                        toolCall.arguments())
                                )
                                .collect(Collectors.joining("\n"));
                        log.info(toolCallInfo);

                        // 使用专用事件类型输出工具调用信息
                        sendStreamEvent(emitter, StreamEventType.TOOL_CALL,
                                "\n[调用工具: " + toolCallInfo + "]");
                        thinkFuture.complete(true);
                    }
                }
        );
        // 异步等待结果
        try {
            return thinkFuture.get(180, TimeUnit.SECONDS); // 设置超时时间
        } catch (Exception e) {
            log.error("等待思考结果超时或出错", e);
            return false;
        }
    }


    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act(SseEmitter emitter) {
        if (toolCallChatResponse == null || !toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
                prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());

        // 当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.
                getLast(toolExecutionResult.conversationHistory());

        // 生成工具响应（去重处理）
        Map<String, String> uniqueResults = new LinkedHashMap<>();
        for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
            String toolName = response.name();
            String responseData = response.responseData();

            // 去重：如果相同工具的结果已存在，则追加而不是覆盖
            if (uniqueResults.containsKey(toolName)) {
                uniqueResults.put(toolName, uniqueResults.get(toolName) + "\n" + responseData);
            } else {
                uniqueResults.put(toolName, responseData);
            }
        }
        // 构建工具响应字符串（格式化输出）
        StringBuilder resultsBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : uniqueResults.entrySet()) {
            resultsBuilder.append("🛠️ 工具 [")
                    .append(entry.getKey())
                    .append("] 执行结果：\n")
                    .append(entry.getValue())
                    .append("\n\n");
        }
        String results = resultsBuilder.toString().trim();
        // 添加空结果检查
        if (StrUtil.isBlank(results)) {
            results = "工具执行完成，但未返回有效结果";
        }
        // 发送工具响应事件（新类型）
        sendStreamEvent(emitter, StreamEventType.TOOL_RESPONSE, results);
        // +++ 将工具结果作为用户消息加入上下文 +++
        if (StrUtil.isNotBlank(results)) {
            getMessageList().add(new UserMessage(results));
        }

        // 生成精简总结
        generateFinalSummary(emitter, results);


        // 判断是否调用了终止工具
        boolean terminateToolCalled = false;
        if (CollUtil.isNotEmpty(toolExecutionResult.conversationHistory())) {
            Message lastMessage = CollUtil.getLast(toolExecutionResult.conversationHistory());
            if (lastMessage instanceof ToolResponseMessage) {
                ToolResponseMessage toolResponse = (ToolResponseMessage) lastMessage;
                for (ToolResponseMessage.ToolResponse response : toolResponse.getResponses()) {
                    if ("doTerminate".equals(response.name())) {
                        terminateToolCalled = true;
                        break; // 找到终止工具后立即跳出循环
                    }
                }
            }
        }
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
            log.info("检测到终止工具调用，设置状态为FINISHED");
        }
        return results;
    }
    //生成最终总结
    protected void generateFinalSummary(SseEmitter emitter, String toolResults) {
        try {
            // 1. 发送总结开始事件
            sendStreamEvent(emitter, StreamEventType.THINKING, "\n🤖 正在生成最终总结...");

            // 2. 构建总结提示词（包含所有上下文）
            String summaryPrompt = "请基于以下对话历史和工具执行结果，生成一个精简的任务总结（不超过100字）：\n\n" +
                    "### 对话历史:\n" + getConversationHistory() + "\n\n" +
                    "### 工具执行结果:\n" + toolResults;

            // 3. 创建总结专用消息列表
            List<Message> summaryMessages = new ArrayList<>();
            summaryMessages.add(new SystemMessage("你是一个专业总结助手，请用简洁的语言总结核心结论"));
            summaryMessages.add(new UserMessage(summaryPrompt));

            // 4. 调用LLM生成总结
            Prompt summaryPromptObj = new Prompt(summaryMessages, chatOptions);
            ChatResponse summaryResponse = getChatClient().prompt(summaryPromptObj)
                    .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                    .call()
                    .chatResponse();

            // 5. 处理并发送总结
            if (summaryResponse.getResult() != null &&
                    summaryResponse.getResult().getOutput() != null) {

                String summary = summaryResponse.getResult().getOutput().getText();
                if (StrUtil.isNotBlank(summary)) {
                    // 作为最终响应发送
                    streamFinalResponse(summary, emitter);
                    markFinalResponseSent();
                    setState(AgentState.FINISHED);
                }
            }
        } catch (Exception e) {
            log.error("总结生成失败", e);
            sendStreamEvent(emitter, StreamEventType.ERROR, "总结生成失败: " + e.getMessage());
            // 失败时发送原始结果作为最终响应
            streamFinalResponse(toolResults, emitter);
            markFinalResponseSent();
        }
    }

}
