package com.ljh.fawnhealth.ai.agent.model;

// 事件类型枚举
public enum StreamEventType {
    THINKING,    // 思考
    TOOL_RESPONSE,  // 工具响应
    TOOL_CALL,      // 工具调用
    FINAL_RESPONSE,     // 最终响应
    ERROR
}