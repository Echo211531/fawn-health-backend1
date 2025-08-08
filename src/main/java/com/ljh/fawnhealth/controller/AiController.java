package com.ljh.fawnhealth.controller;

import cn.hutool.core.util.StrUtil;
import com.ljh.fawnhealth.ai.agent.FawnManus;
import com.ljh.fawnhealth.ai.agent.queue.UserInputQueue;
import com.ljh.fawnhealth.ai.app.HealthApp;
import com.ljh.fawnhealth.ai.app.HealthReportApp;
import com.ljh.fawnhealth.ai.context.AgentContext;
import com.ljh.fawnhealth.ai.store.MongoChatMemory;
import com.ljh.fawnhealth.ai.tool.collection.ToolCollection;
import com.ljh.fawnhealth.commen.BaseResponse;
import com.ljh.fawnhealth.config.ResultUtils;
import com.ljh.fawnhealth.context.BaseContext;
import com.ljh.fawnhealth.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    private HealthApp healthApp;
    @Resource
    private HealthReportApp healthReportApp;

    // 流式调用
    @GetMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChat(String message, String chatId) {
        return healthApp.doChat(message, chatId);
    }

    // 流式调用：设置泛型为 ServerSentEvent，使用这种方式可以省略 MediaType
    @GetMapping(value = "/chat/sse")
    public Flux<ServerSentEvent<String>> doChatWithSSE(String message, String chatId) {
        return healthApp.doChat(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    // SSE 流式调用
    // 通过 send 方法持续向 SseEmitter 发送消息(有点像 IO 操作)
    @GetMapping(value = "/chat/sse_emitter")
    public SseEmitter doChatWithSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        healthApp.doChat(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    // 生成健康报告
    @GetMapping(value = "chat/report")
    public BaseResponse<HealthReportApp.HealthReport> doChatWithReport(String message, String chatId) {
        HealthReportApp.HealthReport report = healthReportApp.doChatWithReport(message, chatId);
        return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), report,
                ErrorCode.SUCCESS.getMessage());
    }

    // RAG检索增强
    @GetMapping(value = "chat/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithRag(String message, String chatId) {
        return healthApp.doChatWithRag(message, chatId);
    }

    // Mcp 服务
    @GetMapping(value = "chat/mcp", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithMcp(String message, String chatId) {
        return healthApp.doChatWithMcp(message, chatId);
    }

    @Resource
    private MongoChatMemory mongoChatMemory;
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ChatModel dashscopeChatModel;

    // 流式调用 Manus 超级智能体（改进版）
    @GetMapping("/chat/manus")
    public SseEmitter doChatWithManus(String message, String chatId) {
        log.info("开始调用FawnManus智能体，消息: {}, chatId: {}", message, chatId);
        try {
            // 检查工具注入
            if (allTools == null) {
                log.error("allTools未正确注入");
                throw new RuntimeException("工具未正确配置");
            }
            log.info("注入的工具数量: {}", allTools.length);

            // 创建工具集合
            ToolCollection toolCollection = new ToolCollection(allTools);
            log.info("工具集合创建成功，工具数量: {}", toolCollection.size());

            // 检查工具集合中的工具
            ToolCallback[] tools = toolCollection.getAllTools();
            if (tools != null) {
                log.info("工具集合中的工具数量: {}", tools.length);
                for (ToolCallback tool : tools) {
                    if (tool != null) {
                        log.debug("工具: {} - {}", tool.getName(), tool.getDescription());
                    }
                }
            } else {
                log.warn("工具集合中的工具为null");
            }

            // 创建AgentContext
            AgentContext context = FawnManus.createContext(message, chatId, toolCollection);
            log.info("AgentContext创建成功");

            // 创建FawnManus智能体
            FawnManus fawnManus = new FawnManus(context, dashscopeChatModel, mongoChatMemory);
            log.info("FawnManus智能体创建成功");

            return fawnManus.runStream(message);
        } catch (Exception e) {
            log.error("创建FawnManus智能体失败", e);
            throw new RuntimeException("智能体创建失败: " + e.getMessage(), e);
        }
    }

    @jakarta.annotation.Resource
    private UserInputQueue userInputQueue;

    /**
     * 询问用户后，用户输入接口后，前端进行调用该接口，把用户输入的内容传递到内容中
     * 
     * @param input 用户输入内容
     * @return
     */
    @GetMapping("/user/input")
    public void userInput(@RequestParam("input") String input) {
        // 回答问题
        try {
            // 非空判断
            if (StrUtil.isBlank(input)) {
                userInputQueue.putResponse("用户输入为空");
            } else {
                userInputQueue.putResponse(input);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据 chatId 获取历史聊天记录
     * 支持参数 lastN 控制获取最近 N 条消息（默认获取全部）
     */
    @GetMapping("/history/{chatId}")
    public BaseResponse<List<Message>> getChatHistory(
            @PathVariable String chatId,
            @RequestParam(defaultValue = "-1") int lastN) {
        int effectiveLastN = lastN <= 0 ? Integer.MAX_VALUE : lastN;
        List<Message> history = mongoChatMemory.get(chatId, effectiveLastN);
        return ResultUtils.success(history);
    }

    /**
     * 获取所有 chatId 列表（用于展示对话历史页面）
     */
    @GetMapping("/conversations")
    public BaseResponse<List<String>> getAllConversations() {
        Long currentUserId = BaseContext.getCurrentId();
        String userId = currentUserId.toString();
        List<String> conversationIds = mongoChatMemory.findAllConversationIds(userId);
        return ResultUtils.success(conversationIds);
    }

    /**
     * 新建对话：生成新的 chatId，并在 MongoDB 中插入一条空记录
     */
    @PostMapping("/conversations/add")
    public BaseResponse<String> createNewConversation() {
        Long currentUserId = BaseContext.getCurrentId();
        String conversationId = currentUserId + "_" + UUID.randomUUID().toString();
        mongoChatMemory.add(conversationId, Collections.emptyList()); // 插入空消息记录
        return ResultUtils.success(conversationId);
    }

    /**
     * 删除指定 chatId 的对话记录
     */
    @DeleteMapping("/conversations/{chatId}")
    public BaseResponse<Boolean> deleteConversation(@PathVariable String chatId) {
        mongoChatMemory.clear(chatId);
        return ResultUtils.success(true);
    }

}
