package com.zr.health.ai.tool;

import jakarta.annotation.Resource;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 集中的工具注册类
 * <p>
 * 本地 {@link ToolCallbacks} 与 Spring AI MCP Client 暴露的 MCP 工具（如 fawn-image-search-mcp-server）合并后，
 * 供 {@link com.zr.health.ai.agent.FawnManus} 使用。
 */
@Configuration
public class ToolRegistration {

    @Resource
    DietTool dietTool;
    @Resource
    DatabaseOperationTool databaseOperationTool;
    @Resource
    FileOperationTool fileOperationTool;
    @Resource
    ZhipuWebSearchTool zhipuWebSearchTool;
    @Resource
    QueryUserTool queryUserTool;
    @Resource
    SaveAsMarkdownTool saveAsMarkdownTool;
    @Resource
    UserInteractionTool userInteractionTool;

    /**
     * 注册所有 AI 工具（本地工具 + MCP 子进程暴露的工具，如图像搜索）
     *
     * @param mcpToolCallbackProvider MCP 客户端聚合的 ToolCallbackProvider，未配置时可为空
     * @return 合并后的工具回调数组
     */
    @Bean
    public ToolCallback[] allTools(ObjectProvider<ToolCallbackProvider> mcpToolCallbackProvider) {
        ToolCallback[] localTools = ToolCallbacks.from(
                dietTool,
                zhipuWebSearchTool,
                queryUserTool,
                saveAsMarkdownTool,
                new WebScrapingTool(),
                fileOperationTool,
                databaseOperationTool,
                new ResourceDownloadTool(),
                new PDFGenerationTool(),
                new DateTimeTool(),
                new TerminateTool()
                // userInteractionTool
        );

        List<ToolCallback> merged = new ArrayList<>(Arrays.asList(localTools));
        mcpToolCallbackProvider.ifAvailable(provider -> {
            // Spring AI M6 文档中 getToolCallbacks() 返回 FunctionCallback[]，运行时均为 ToolCallback 实现
            FunctionCallback[] fromMcp = provider.getToolCallbacks();
            if (fromMcp == null || fromMcp.length == 0) {
                return;
            }
            for (FunctionCallback fc : fromMcp) {
                if (fc instanceof ToolCallback) {
                    merged.add((ToolCallback) fc);
                }
            }
        });
        return merged.toArray(new ToolCallback[0]);
    }
}