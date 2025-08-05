package com.ljh.fawnhealth.ai.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 */
public class DateTimeTool {

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 获取当前日期时间
     */
    @Tool(description = "获取当前日期和时间，并按指定格式返回")
    public String getCurrentDateTime(
            @ToolParam(description = "格式化表达式（例如：yyyy-MM-dd HH:mm:ss）") String format
    ) {
        try {
            String formatPattern = (format == null || format.isEmpty()) ? DEFAULT_DATETIME_FORMAT : format;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(formatPattern);
            return LocalDateTime.now().format(formatter);
        } catch (Exception e) {
            return "获取当前日期时间时发生错误：" + e.getMessage();
        }
    }
}