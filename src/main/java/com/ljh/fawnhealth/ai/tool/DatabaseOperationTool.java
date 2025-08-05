package com.ljh.fawnhealth.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作工具类
 */
@Component
public class DatabaseOperationTool {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询数据（不带参数）
     */
    @Tool(description = "执行SQL语句查询数据库（无参数）")
    public String queryData(@ToolParam(description = "要执行的SQL查询语句") String sql) {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            return formatResultList(result);
        } catch (Exception e) {
            return "执行查询时发生错误：" + e.getMessage();
        }
    }

    /**
     * 查询数据（带参数）
     */
    @Tool(description = "执行带参数的SQL查询语句")
    public String queryData1(
            @ToolParam(description = "要执行的SQL查询语句") String sql,
            @ToolParam(description = "查询参数（JSON格式），例如：{\"id\":1, \"name\":\"test\"}") String params
    ) {
        try {
            Map<String, Object> paramMap = parseParams(params);
            List<Map<String, Object>> result = namedParameterJdbcTemplate.queryForList(sql, paramMap);
            return formatResultList(result);
        } catch (Exception e) {
            return "执行查询时发生错误：" + e.getMessage();
        }
    }

    /**
     * 插入数据
     */
    @Tool(description = "向数据库插入数据")
    public String insertData(
            @ToolParam(description = "INSERT语句") String sql,
            @ToolParam(description = "插入参数（JSON格式）") String params
    ) {
        try {
            Map<String, Object> paramMap = parseParams(params);
            int rowsAffected = namedParameterJdbcTemplate.update(sql, paramMap);
            return "成功插入 " + rowsAffected + " 条记录";
        } catch (Exception e) {
            return "插入数据时发生错误：" + e.getMessage();
        }
    }

    /**
     * 更新数据
     */
    @Tool(description = "更新数据库中的数据")
    public String updateData(
            @ToolParam(description = "UPDATE语句") String sql,
            @ToolParam(description = "更新参数（JSON格式）") String params
    ) {
        try {
            Map<String, Object> paramMap = parseParams(params);
            int rowsAffected = namedParameterJdbcTemplate.update(sql, paramMap);
            return "成功更新 " + rowsAffected + " 条记录";
        } catch (Exception e) {
            return "更新数据时发生错误：" + e.getMessage();
        }
    }

    /**
     * 删除数据
     */
    @Tool(description = "从数据库中删除数据")
    public String deleteData(
            @ToolParam(description = "DELETE语句") String sql,
            @ToolParam(description = "删除参数（JSON格式）") String params
    ) {
        try {
            Map<String, Object> paramMap = parseParams(params);
            int rowsAffected = namedParameterJdbcTemplate.update(sql, paramMap);
            return "成功删除 " + rowsAffected + " 条记录";
        } catch (Exception e) {
            return "删除数据时发生错误：" + e.getMessage();
        }
    }

    /**
     * 解析JSON参数
     */
    private Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            System.err.println("解析JSON参数时出错：" + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 格式化查询结果
     */
    private String formatResultList(List<Map<String, Object>> resultList) {
        if (resultList.isEmpty()) {
            return "未找到任何数据";
        }

        StringBuilder sb = new StringBuilder();

        // 添加表头
        Map<String, Object> firstRow = resultList.get(0);
        sb.append(String.join(" | ", firstRow.keySet())).append("\n");

        // 创建分隔线
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < sb.length() - 1; i++) {
            separator.append("-");
        }
        sb.append(separator).append("\n");

        // 添加数据行
        for (Map<String, Object> row : resultList) {
            sb.append(row.values().stream()
                    .map(val -> val == null ? "NULL" : val.toString())
                    .reduce((a, b) -> a + " | " + b)
                    .orElse(""))
                    .append("\n");
        }

        return sb.toString();
    }
}