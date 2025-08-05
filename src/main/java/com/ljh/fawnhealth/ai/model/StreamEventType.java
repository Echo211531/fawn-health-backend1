package com.ljh.fawnhealth.ai.model;

/**
 * 流式事件类型枚举
 */
public enum StreamEventType {
    THINKING,           // 思考过程
    TOOL_CALL,          // 工具调用
    TOOL_RESPONSE,      // 工具响应
    FINAL_RESPONSE,     // 最终响应
    ERROR               // 错误信息
}