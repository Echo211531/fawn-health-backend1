package com.zr.health.ai.context;

import com.zr.health.ai.tool.collection.ToolCollection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 智能体上下文环境
 */
@Data
@Builder
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class AgentContext {
    private String requestId; // 请求ID
    private String chatId; // 会话ID
    private String query; // 用户查询
    private String task; // 任务描述
    private ToolCollection toolCollection; // 工具集合
    private String dateInfo; // 日期信息
    private List<String> productFiles; // 产品文件列表
    private Boolean isStream; // 是否流式输出
    private String streamMessageType; // 流式消息类型
    private String basePrompt; // 基础提示词
    private Integer agentType; // 智能体类型

}