package com.ljh.fawnhealth.handler;

import com.ljh.fawnhealth.mapper.CouponsMapper;
import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class PromotionMqHandler {

    @Resource
    private UserCouponService userCouponService;
    @Resource
    private CouponsMapper couponsMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.FH_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MqConstant.FH_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstant.FH_ROUTING_KEY
    ))
    public void listenCouponReceiveMessage(UserCouponDTO uc) {
        try {
            Long couponId = uc.getCouponId();
            Coupons coupons = couponsMapper.selectById(couponId);
            Long userId = uc.getUserId();
            userCouponService.checkAndCreateUserCoupon(coupons, userId, null);
        } catch (Exception e) {
            // 记录日志
            System.err.println("处理消息时出现异常: " + e.getMessage());
            // 可以根据具体情况进行其他处理，如重试、发送告警等
        }
    }
}