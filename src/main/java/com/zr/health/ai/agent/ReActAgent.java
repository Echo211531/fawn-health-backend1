package com.zr.health.ai.agent;

import com.zr.health.ai.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类  
 * 实现了思考-行动的循环模式  
 */  
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {
    // 添加默认构造函数解决 Lombok 要求
    public ReActAgent() {
        super(null); // 传递 null 给父类
    }
    // 添加带 chatId 的构造函数
    public ReActAgent(String chatId) {
        super(chatId);
    }
    /**
     * 流式步骤执行
     */
    @Override
    protected void streamStep(SseEmitter emitter) throws Exception {
        // 添加状态检查
        if (getState() == AgentState.FINISHED) {
            log.info("状态已为FINISHED，跳过步骤");
            return;
        }
        // 1. 思考阶段
        boolean shouldAct = think(emitter);
        // 2. 执行阶段（如果需要）
        if (shouldAct && getState() != AgentState.FINISHED) {
            act(emitter);
        }
    }
    /**
     * 处理当前状态并决定下一步行动  
     *  
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行  
     */  
    public abstract boolean think(SseEmitter emitter) throws Exception;
  
    /**  
     * 执行决定的行动  
     *  
     * @return 行动执行结果  
     */  
    public abstract String act(SseEmitter emitter);

}