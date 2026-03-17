package com.zr.health.handler;

import com.zr.health.constant.PromotionConstants;
import com.zr.health.mapper.CommentLikesMapper;
import com.zr.health.mapper.CouponsMapper;
import com.rabbitmq.client.Channel;
import com.zr.health.model.dto.comments.LikeEventDTO;
import com.zr.health.model.dto.coupons.UserCouponDTO;
import com.zr.health.model.entity.Coupons;
import com.zr.health.mq.MqConstant;
import com.zr.health.service.UserCouponService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
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
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 优惠券消息消费者（带幂等性保障 + 数据库库存更新）
     * 1. 通过数据库唯一索引保证消息幂等性
     * 2. 在消费者中更新数据库库存，保证最终一致性
     *
     * @param couponDTO
     * @param channel
     * @param deliveryTag
     */
    @RabbitListener(queues = MqConstant.FH_QUEUE_NAME)
    public void handleCouponMessage(UserCouponDTO couponDTO, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        String stockKey = PromotionConstants.COUPON_CACHE_KEY_PREFIX + couponDTO.getCouponId() + ":stock";
        String userKey = PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + couponDTO.getCouponId();
        
        try {
            log.info("收到优惠券消息: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());

            // 1. 查询优惠券信息
            Coupons coupon = couponsMapper.selectById(couponDTO.getCouponId());
            if (coupon == null) {
                log.error("优惠券不存在: couponId={}", couponDTO.getCouponId());
                // 回滚 Redis
                rollbackRedis(stockKey, userKey, couponDTO.getUserId());
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 2. 数据库原子更新库存（防止超卖，保证最终一致性）
            int updateCount = couponsMapper.incrIssueNum(couponDTO.getCouponId());
            if (updateCount == 0) {
                // 数据库库存不足，回滚 Redis
                log.warn("数据库库存不足，回滚 Redis: userId={}, couponId={}", 
                         couponDTO.getUserId(), couponDTO.getCouponId());
                rollbackRedis(stockKey, userKey, couponDTO.getUserId());
                channel.basicAck(deliveryTag, false);  // 确认消息，避免重复处理
                return;
            }

            // 3. 创建用户券记录（数据库唯一索引保证幂等性）
            userCouponService.checkAndCreateUserCoupon(coupon, couponDTO.getUserId(), null);

            // 4. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("优惠券消息处理成功: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());
            
        } catch (DuplicateKeyException e) {
            // 幂等性处理：用户券已存在，但数据库库存已更新，直接确认消息
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
                // 处理失败，回滚 Redis
                rollbackRedis(stockKey, userKey, couponDTO.getUserId());
                // 拒绝消息并发送到死信队列（可以重试）
                channel.basicNack(deliveryTag, false, true);  // requeue=true，重新入队
            } catch (IOException ex) {
                log.error("发送 NACK 失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 回滚 Redis 操作
     */
    private void rollbackRedis(String stockKey, String userKey, Long userId) {
        try {
            // 回滚库存
            redisTemplate.opsForValue().increment(stockKey);
            // 回滚用户限领数量
            redisTemplate.opsForHash().increment(userKey, userId.toString(), -1);
            log.info("Redis 回滚成功: userId={}, stockKey={}", userId, stockKey);
        } catch (Exception e) {
            log.error("Redis 回滚失败: userId={}, stockKey={}", userId, stockKey, e);
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