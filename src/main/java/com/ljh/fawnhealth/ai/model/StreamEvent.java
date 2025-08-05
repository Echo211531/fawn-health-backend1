package com.ljh.fawnhealth.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式事件数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {
    private StreamEventType type;  //事件类型
    private String content;  //事件内容
    private long timestamp;  //时间戳

    public StreamEvent(StreamEventType type, String content) {
        this.type = type;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
}