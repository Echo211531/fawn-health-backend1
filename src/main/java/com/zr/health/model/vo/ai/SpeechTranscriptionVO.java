package com.zr.health.model.vo.ai;

import lombok.Data;

/**
 * 语音转文字结果视图对象
 * 用于封装一次语音识别的核心结果，便于接口统一返回
 */
@Data
public class SpeechTranscriptionVO {

    /**
     * 本次语音识别会话ID
     * 用于问题排查及日志关联
     */
    private String sessionId;

    /**
     * 识别出的完整文本内容
     */
    private String text;

    /**
     * 原始返回结果JSON字符串（可选）
     * 便于前端或运维在需要时做更精细的解析和排查
     */
    private String rawResultJson;
}

