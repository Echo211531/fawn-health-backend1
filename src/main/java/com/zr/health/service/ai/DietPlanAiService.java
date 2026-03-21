package com.zr.health.service.ai;

import com.zr.health.exception.BusinessException;
import com.zr.health.exception.ErrorCode;
import com.zr.health.model.dto.ai.DietPlanRequestDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 基于大模型的饮食计划生成服务（无会话记忆，单次调用）。
 */
@Slf4j
@Service
public class DietPlanAiService {

    private static final String SYSTEM_PROMPT = """
            你是「小鹿健康」应用中的注册营养顾问助手。请始终使用简体中文回复，语气友好、专业、简洁。
            你只提供一般性的膳食搭配思路，不得替代医生或临床营养师。禁止编造用户不存在的体检数据或疾病诊断。
            """;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 根据体重目标与周期生成一日饮食建议（Markdown）。
     *
     * @param dto 请求参数
     * @return AI 生成的 Markdown 文本
     */
    public String generateDietPlan(DietPlanRequestDTO dto) {
        validate(dto);
        String current = Optional.ofNullable(dto.getCurrentWeight())
                .map(BigDecimal::stripTrailingZeros)
                .map(BigDecimal::toPlainString)
                .orElse("未提供");
        String target = dto.getTargetWeight().stripTrailingZeros().toPlainString();
        int days = dto.getPeriodDays();
        String kcal = Optional.ofNullable(dto.getDailyCalories())
                .map(BigDecimal::stripTrailingZeros)
                .map(BigDecimal::toPlainString)
                .orElse("未提供，请结合健康减重/增重的一般原则估算");

        String userPrompt = String.format("""
                请根据以下用户信息，生成一份**可执行的单日饮食搭配建议**（用户可多日重复参考，适当变换食材），使用 Markdown 标题与列表排版。
                
                - 当前体重（kg）：%s
                - 目标体重（kg）：%s
                - 计划达成天数：%d 天
                - 每日建议摄入热量（大卡）：%s
                
                输出结构要求：
                1. 先用一小段话概括：在 %d 天内从当前体重到目标体重，节奏是否偏快/适中/偏慢（非医疗判断，仅作常识提醒）。
                2. 「早餐」「午餐」「晚餐」「加餐」四节，每节列出 2～4 种具体食物及大致分量（以中式家常为主）。
                3. 「一日营养原则」：控油控盐、蛋白质与蔬菜、饮水量等 3～5 条。
                4. 最后一行单独一段免责声明：本建议仅供参考，不替代医疗诊断；孕妇、慢性病患者及用药人群请咨询医生或临床营养师。
                """, current, target, days, kcal, days);

        try {
            return ChatClient.builder(dashscopeChatModel)
                    .defaultSystem(SYSTEM_PROMPT)
                    .build()
                    .prompt()
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("生成 AI 饮食计划失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "智能饮食计划生成失败，请稍后重试");
        }
    }

    private void validate(DietPlanRequestDTO dto) {
        if (dto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求体不能为空");
        }
        if (dto.getTargetWeight() == null || dto.getTargetWeight().compareTo(new BigDecimal("5")) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标体重不能低于 5kg");
        }
        if (dto.getPeriodDays() == null || dto.getPeriodDays() < 1 || dto.getPeriodDays() > 3650) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "目标天数须在 1～3650 天之间");
        }
        if (dto.getCurrentWeight() != null && dto.getCurrentWeight().compareTo(new BigDecimal("1")) < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前体重数据无效");
        }
        if (dto.getDailyCalories() != null && dto.getDailyCalories().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每日热量预算须大于 0");
        }
    }
}
