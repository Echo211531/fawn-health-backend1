package com.ljh.fawnhealth.config;

import com.ljh.fawnhealth.ai.agent.BaseAgent;
import com.ljh.fawnhealth.ai.store.ChatStreamEventStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * 智能体配置类
 */
@Configuration
@Slf4j
public class AgentConfiguration {

    @Resource
    private ChatStreamEventStore chatStreamEventStore;

    /**
     * 初始化BaseAgent的静态依赖
     */
    @PostConstruct
    public void initBaseAgent() {
        BaseAgent.setChatStreamEventStore(chatStreamEventStore);
        log.info("BaseAgent静态依赖初始化完成");
    }
}