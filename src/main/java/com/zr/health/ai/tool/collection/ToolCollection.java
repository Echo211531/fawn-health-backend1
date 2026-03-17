package com.zr.health.ai.tool.collection;
import org.springframework.ai.tool.ToolCallback;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具集合管理类
 */
@Data
@Slf4j
public class ToolCollection {
    private Map<String, ToolCallback> toolMap = new HashMap<>();
    public ToolCollection() {}
    public ToolCollection(ToolCallback[] tools) {
        if (tools != null) {
            for (ToolCallback tool : tools) {
                addTool(tool);
            }
        }
    }
    /**
     * 添加工具
     */
    public void addTool(ToolCallback tool) {
        if (tool != null) {
            toolMap.put(tool.getName(), tool);
            log.info("添加工具: {}", tool.getName());
        }
    }
    /**
     * 获取工具
     */
    public ToolCallback getTool(String name) {
        return toolMap.get(name);
    }
    
    /**
     * 获取所有工具
     */
    public ToolCallback[] getAllTools() {
        return toolMap.values().toArray(new ToolCallback[0]);
    }
    
    /**
     * 获取工具数量
     */
    public int size() {
        return toolMap.size();
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return toolMap.containsKey(name);
    }
}