package com.ljh.fawnhealth.listener;

import com.ljh.fawnhealth.events.DietRecordCreatedEvent;
import com.ljh.fawnhealth.mq.MessageProducer;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.model.vo.rule.HealthRiskWarningVO;
import com.ljh.fawnhealth.service.RuleEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class HealthRiskEvaluateListener {

    private final RuleEngineService ruleEngineService;
    private final MessageProducer messageProducer;

    public HealthRiskEvaluateListener(RuleEngineService ruleEngineService, MessageProducer messageProducer) {
        this.ruleEngineService = ruleEngineService;
        this.messageProducer = messageProducer;
    }

    @Async
    @EventListener
    public void onDietRecordCreated(DietRecordCreatedEvent event) {
        Long userId = event.getUserId();
        try {
            List<HealthRiskWarningVO> warnings = ruleEngineService.evaluateUserHealthRisk(String.valueOf(userId));
            if (warnings != null && !warnings.isEmpty()) {
                // 通过主交换机推送预警结果，使用自定义的路由键
                messageProducer.sendMessage(MqConstant.FH_EXCHANGE_NAME, "fh.key.health.warning", warnings);
            }
        } catch (Exception e) {
            log.error("异步评估健康风险失败, userId={}", userId, e);
        }
    }
}