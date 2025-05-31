package com.ljh.fawnhealth.ai.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类
 */
@Configuration
public class ToolRegistration {


    @Resource
    DatabaseOperationTool databaseOperationTool;
    @Resource
    FileOperationTool fileOperationTool;
    @Resource
    ZhipuWebSearchTool zhipuWebSearchTool;
    @Resource
    QueryUserTool queryUserTool;
    /**
     * 注册所有AI工具
     */
    @Bean
    public ToolCallback[] allTools() {
        // 实例化所有工具
        return ToolCallbacks.from(
                zhipuWebSearchTool,
                queryUserTool,
                new WebScrapingTool(),
                fileOperationTool,
                databaseOperationTool,
                new ResourceDownloadTool(),
                new PDFGenerationTool(),
                new DateTimeTool(),
                new TerminateTool()
        );
    }
}