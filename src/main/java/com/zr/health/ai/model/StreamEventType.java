package com.zr.health.ai.model;

/**
 * 流式事件类型枚举
 */
public enum StreamEventType {
    USER_MESSAGE,       // 用户消息
    THINKING,           // 思考过程
    TOOL_CALL,          // 工具调用
    TOOL_RESPONSE,      // 工具响应
    FINAL_RESPONSE,     // 最终响应
    FINAL,              // 最终响应（简化版）
    ERROR               // 错误信息
}