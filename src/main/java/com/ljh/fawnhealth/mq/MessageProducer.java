package com.ljh.fawnhealth.mq;


//import com.ljh.fawnhealth.config.RabbitMQConfig;
import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;



@Component
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param exchange
     * @param routingKey
     * @param userCouponDTO
     */
    public void sendMessage(String exchange, String routingKey, UserCouponDTO userCouponDTO) {
        rabbitTemplate.convertAndSend(exchange, routingKey, userCouponDTO);
    }

}
