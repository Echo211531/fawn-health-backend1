package com.ljh.fawnhealth.ai.util;

import cn.hutool.core.util.StrUtil;
import com.ljh.fawnhealth.ai.model.StreamEvent;
import com.ljh.fawnhealth.ai.model.StreamEventType;
import com.ljh.fawnhealth.ai.store.ChatStreamEventStore;
import com.ljh.fawnhealth.context.BaseContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * 聊天流式输出工具类
 * 用于封装流式输出时的消息保存逻辑
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ChatStreamHelper {

    private final ChatStreamEventStore chatStreamEventStore;

    /**
     * 保存用户消息到流式事件存储
     *
     * @param message 用户消息
     * @param chatId  会话ID
     */
    public void saveUserMessage(String message, String chatId) {
        if (StrUtil.isBlank(message) || StrUtil.isBlank(chatId)) {
            return;
        }
        try {
            Long currentUserId = BaseContext.getCurrentId();
            String userId = currentUserId != null ? currentUserId.toString() : "unknown";
            StreamEvent userMessageEvent = new StreamEvent(StreamEventType.USER_MESSAGE, message);
            chatStreamEventStore.saveStreamEvent(chatId, userMessageEvent, userId);
            log.debug("保存用户消息到流式事件存储: chatId={}, message={}", chatId, message);
        } catch (Exception e) {
            log.error("保存用户消息到流式事件存储失败: chatId={}", chatId, e);
        }
    }

    /**
     * 保存AI响应到流式事件存储
     *
     * @param response AI响应内容
     * @param chatId   会话ID
     */
    public void saveAiResponse(String response, String chatId) {
        if (StrUtil.isBlank(response) || StrUtil.isBlank(chatId)) {
            return;
        }
        try {
            Long currentUserId = BaseContext.getCurrentId();
            String userId = currentUserId != null ? currentUserId.toString() : "unknown";
            StreamEvent aiResponseEvent = new StreamEvent(StreamEventType.FINAL_RESPONSE, response);
            chatStreamEventStore.saveStreamEvent(chatId, aiResponseEvent, userId);
            log.debug("保存AI响应到流式事件存储: chatId={}, responseLength={}", chatId, response.length());
        } catch (Exception e) {
            log.error("保存AI响应到流式事件存储失败: chatId={}", chatId, e);
        }
    }

    /**
     * 为Flux流式输出添加消息保存功能
     * 自动收集完整响应并在完成后保存
     *
     * @param flux   原始Flux流
     * @param chatId 会话ID
     * @return 增强后的Flux流
     */
    public Flux<String> enhanceFluxWithSave(Flux<String> flux, String chatId) {
        StringBuilder responseBuilder = new StringBuilder();
        return flux
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        responseBuilder.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    String fullResponse = responseBuilder.toString();
                    if (StrUtil.isNotBlank(fullResponse)) {
                        saveAiResponse(fullResponse, chatId);
                    }
                })
                .doOnError(e -> {
                    log.error("流式输出错误: chatId={}", chatId, e);
                    // 即使出错，也保存已收集的响应
                    String partialResponse = responseBuilder.toString();
                    if (StrUtil.isNotBlank(partialResponse)) {
                        saveAiResponse(partialResponse, chatId);
                    }
                });
    }

    /**
     * 为SseEmitter订阅添加消息保存功能
     * 自动收集完整响应并在完成后保存
     *
     * @param flux       原始Flux流
     * @param sseEmitter SseEmitter实例
     * @param chatId     会话ID
     */
    public void subscribeWithSave(Flux<String> flux, SseEmitter sseEmitter, String chatId) {
        StringBuilder responseBuilder = new StringBuilder();
        flux.subscribe(
                chunk -> {
                    try {
                        // 收集响应
                        if (chunk != null) {
                            responseBuilder.append(chunk);
                        }
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                },
                error -> {
                    log.error("SseEmitter流式输出错误: chatId={}", chatId, error);
                    // 即使出错，也保存已收集的响应
                    String partialResponse = responseBuilder.toString();
                    if (StrUtil.isNotBlank(partialResponse)) {
                        saveAiResponse(partialResponse, chatId);
                    }
                    sseEmitter.completeWithError(error);
                },
                () -> {
                    // 流式输出完成后，保存完整响应
                    String fullResponse = responseBuilder.toString();
                    if (StrUtil.isNotBlank(fullResponse)) {
                        saveAiResponse(fullResponse, chatId);
                    }
                    sseEmitter.complete();
                }
        );
    }
}

