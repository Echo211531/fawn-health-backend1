package com.zr.health.ai.tool;

import org.springframework.ai.tool.annotation.Tool;

//终止工具（让智能体自主合理中断）
public class TerminateTool {

    @Tool(description = """
            当请求已完成 或 助手无法继续执行任务时，调用此工具以终止交互。
            "当您已完成所有任务时，请调用此工具以结束工作。"
            """)
    public String doTerminate() {
        return "任务结束";
    }
}