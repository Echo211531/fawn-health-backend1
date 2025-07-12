package com.ljh.fawnhealth.ai.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;

// 流式响应DTO
@Data
@AllArgsConstructor
public class StreamEvent {
    private StreamEventType type; //类型
    private String content;     //内容
}
