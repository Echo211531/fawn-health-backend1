package com.ljh.fawnhealth.handler;

import com.ljh.fawnhealth.mapper.CommentLikesMapper;
import com.ljh.fawnhealth.mapper.CouponsMapper;
import com.rabbitmq.client.Channel;
import com.ljh.fawnhealth.model.dto.comments.LikeEventDTO;
import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PromotionMqHandler {

    @Resource
    private UserCouponService userCouponService;
    @Resource
    private CouponsMapper couponsMapper;
    @Resource
    private CommentLikesMapper commentLikesMapper;

    /**
     * 优惠券消息消费者（带幂等性保障）
     * 通过数据库唯一索引保证消息幂等性，即使消息重复消费也不会重复创建用户券
     *
     * @param couponDTO
     * @param channel
     * @param deliveryTag
     */
    @RabbitListener(queues = MqConstant.FH_QUEUE_NAME)
    public void handleCouponMessage(UserCouponDTO couponDTO, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到优惠券消息: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());

            // 1. 查询优惠券信息
            Coupons coupon = couponsMapper.selectById(couponDTO.getCouponId());
            if (coupon == null) {
                log.error("优惠券不存在: couponId={}", couponDTO.getCouponId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 业务逻辑处理（数据库唯一索引保证幂等性）
            userCouponService.checkAndCreateUserCoupon(coupon, couponDTO.getUserId(), null);

            // 3. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("优惠券消息处理成功: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());
            
        } catch (DuplicateKeyException e) {
            // 幂等性处理：用户券已存在，直接确认消息（不重复处理）
            log.info("用户券已存在，跳过处理（幂等）: userId={}, couponId={}", 
                     couponDTO.getUserId(), couponDTO.getCouponId());
            try {
                channel.basicAck(deliveryTag, false);
            } catch (IOException ex) {
                log.error("确认消息失败", ex);
            }
        } catch (Exception e) {
            log.error("处理优惠券消息失败: userId={}, couponId={}, error={}", 
                     couponDTO.getUserId(), couponDTO.getCouponId(), e.getMessage(), e);
            try {
                // 拒绝消息并发送到死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("发送 NACK 失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 点赞消息消费者
     *
     * @param likeEvent
     * @param channel
     * @param deliveryTag
     */
    @RabbitListener(queues = MqConstant.COMMENT_LIKE_QUEUE_NAME)
    public void handleLikeMessage(LikeEventDTO likeEvent, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("收到点赞消息: {}", likeEvent);

            // 业务逻辑处理
            if (likeEvent.getLiked() != null) {
                if (likeEvent.getLiked()) {
                    commentLikesMapper.upsertLike(likeEvent.getCommentId(), likeEvent.getUserId());
                } else {
                    commentLikesMapper.markDeleted(likeEvent.getCommentId(), likeEvent.getUserId());
                }
            }

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("点赞消息处理成功");
        } catch (Exception e) {
            log.error("处理点赞消息失败: {}", e.getMessage(), e);
            try {
                // 拒绝消息并发送到死信队列
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ex) {
                log.error("发送 NACK 失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 优惠券死信队列消费者
     *
     * @param message
     */
    @RabbitListener(queues = MqConstant.FH_DLQ_QUEUE_NAME)
    public void handleCouponDlqMessage(Message message) {
        try {
            String body = new String(message.getBody());
            log.error("优惠券死信消息: {}", body);

            // 获取异常信息
            String exceptionMessage = (String) message.getMessageProperties().getHeaders().get("x-exception-message");
            String stackTrace = (String) message.getMessageProperties().getHeaders().get("x-exception-stacktrace");

            log.error("异常原因: {}", exceptionMessage);
            if (stackTrace != null) {
                log.error("堆栈信息: {}", stackTrace);
            }
            //todo 可以添加人工干预逻辑，如发送邮件、短信通知
        } catch (Exception e) {
            log.error("处理优惠券死信消息异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 点赞死信队列消费者
     *
     * @param message
     */
    @RabbitListener(queues = MqConstant.LIKE_DLQ_QUEUE_NAME)
    public void handleLikeDlqMessage(Message message) {
        try {
            String body = new String(message.getBody());
            log.error("点赞死信消息: {}", body);

            // 获取异常信息
            String exceptionMessage = (String) message.getMessageProperties().getHeaders().get("x-exception-message");
            String stackTrace = (String) message.getMessageProperties().getHeaders().get("x-exception-stacktrace");

            log.error("异常原因: {}", exceptionMessage);
            if (stackTrace != null) {
                log.error("堆栈信息: {}", stackTrace);
            }
            //todo 可以添加人工干预逻辑，如发送邮件、短信通知
        } catch (Exception e) {
            log.error("处理点赞死信消息异常: {}", e.getMessage(), e);
        }
    }
}