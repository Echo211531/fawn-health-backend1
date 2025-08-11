package com.ljh.fawnhealth.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ljh.fawnhealth.mapper.UserMapper;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.mq.MessageProducer;
import com.ljh.fawnhealth.model.entity.User;
import com.ljh.fawnhealth.model.vo.rule.HealthRiskWarningVO;
import com.ljh.fawnhealth.service.RuleEngineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class HealthRiskScheduledScanner {

    private final UserMapper userMapper;
    private final RuleEngineService ruleEngineService;
    private final MessageProducer messageProducer;

    public HealthRiskScheduledScanner(UserMapper userMapper,
            RuleEngineService ruleEngineService,
            MessageProducer messageProducer) {
        this.userMapper = userMapper;
        this.ruleEngineService = ruleEngineService;
        this.messageProducer = messageProducer;
    }

    // 每小时扫描一次（可按需调整为每日固定时刻）
    @Scheduled(cron = "0 0 * * * ?")
    public void scanAllUsersAndEvaluate() {
        log.info("[HealthRiskScheduledScanner] 定时扫描开始");
        long pageNum = 1;
        long pageSize = 500;
        while (true) {
            Page<User> page = new Page<>(pageNum, pageSize);
            Page<User> result = userMapper.selectPage(page,
                    new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
            List<User> users = result.getRecords();
            if (users == null || users.isEmpty()) {
                break;
            }
            for (User user : users) {
                Long userId = user.getId();
                try {
                    List<HealthRiskWarningVO> warnings = ruleEngineService
                            .evaluateUserHealthRisk(String.valueOf(userId));
                    if (warnings != null && !warnings.isEmpty()) {
                        messageProducer.sendMessage(MqConstant.FH_EXCHANGE_NAME, "fh.key.health.warning", warnings);
                        log.info("[HealthRiskScheduledScanner] 用户{} 触发{}条预警，已推送MQ", userId, warnings.size());
                    }
                } catch (Exception ex) {
                    log.error("[HealthRiskScheduledScanner] 评估用户{} 失败", userId, ex);
                }
            }
            if (pageNum >= result.getPages()) {
                break;
            }
            pageNum++;
        }
        log.info("[HealthRiskScheduledScanner] 定时扫描结束");
    }
}