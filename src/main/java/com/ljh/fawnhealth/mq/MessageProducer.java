package com.ljh.fawnhealth.mq;

import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public MessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送消息
     *
     * @param exchange
     * @param routingKey
     * @param userCouponDTO
     */
    public void sendMessage(String exchange, String routingKey, UserCouponDTO userCouponDTO) {
        log.info("发送点赞消息到 MQ: {}", userCouponDTO); // 添加日志
        rabbitTemplate.convertAndSend(exchange, routingKey, userCouponDTO);
    }

    /**
     * 发送任何类型消息
     *
     * @param exchange
     * @param routingKey
     * @param message
     */
    public void sendMessage(String exchange, String routingKey, Object message) {
        log.info("发送点赞消息到 MQ: {}", message); // 添加日志
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    /**
     * 发送优惠券消息
     *
     * @param routingKey
     * @param message
     */
    public void sendCouponMessage(String routingKey, Object message) {
        log.info("发送优惠券消息到 MQ，路由键: {}，消息: {}", routingKey, message);
        rabbitTemplate.convertAndSend(MqConstant.FH_EXCHANGE_NAME, routingKey, message);
    }

    /**
     * 发送点赞消息
     *
     * @param routingKey
     * @param message
     */
    public void sendLikeMessage(String routingKey, Object message) {
        log.info("发送点赞消息到 MQ，路由键: {}，消息: {}", routingKey, message);
        rabbitTemplate.convertAndSend(MqConstant.FH_EXCHANGE_NAME, routingKey, message);
    }
}
