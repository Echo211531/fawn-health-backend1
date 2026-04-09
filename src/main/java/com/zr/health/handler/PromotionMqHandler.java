package com.zr.health.handler;

import com.zr.health.constant.PromotionConstants;
import com.zr.health.exception.BusinessException;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

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
    private StringRedisTemplate stringRedisTemplate;

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
        String requestId = couponDTO.getRequestId();
        String requestKey = requestId == null ? null : (PromotionConstants.USER_COUPON_CACHE_KEY_PREFIX + "req:" + requestId);
        
        try {
            log.info("收到优惠券消息: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());
            if (requestKey != null) {
                Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(requestKey, "1", Duration.ofDays(1));
                if (Boolean.FALSE.equals(first)) {
                    channel.basicAck(deliveryTag, false);
                    return;
                }
            }

            // 1. 查询优惠券信息
            Coupons coupon = couponsMapper.selectById(couponDTO.getCouponId());
            if (coupon == null) {
                log.error("优惠券不存在: couponId={}", couponDTO.getCouponId());
                // 回滚 Redis
                rollbackRedis(stockKey, userKey, couponDTO.getUserId());
                if (requestKey != null) {
                    stringRedisTemplate.delete(requestKey);
                }
                channel.basicAck(deliveryTag, false);
                return;
            }

            userCouponService.checkAndCreateUserCoupon(coupon, couponDTO.getUserId(), null);

            // 3. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("优惠券消息处理成功: userId={}, couponId={}", couponDTO.getUserId(), couponDTO.getCouponId());
            
        } catch (DuplicateKeyException e) {
            log.warn("用户券写入重复键（请检查 user_coupon 是否存在唯一索引 uk_user_coupon）：userId={}, couponId={}",
                    couponDTO.getUserId(), couponDTO.getCouponId());
            rollbackRedis(stockKey, userKey, couponDTO.getUserId());
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
                if (e instanceof BusinessException) {
                    channel.basicAck(deliveryTag, false);
                } else {
                    if (requestKey != null) {
                        stringRedisTemplate.delete(requestKey);
                    }
                    channel.basicNack(deliveryTag, false, true);
                }
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
            stringRedisTemplate.opsForValue().increment(stockKey);
            // 回滚用户限领数量
            stringRedisTemplate.opsForHash().increment(userKey, userId.toString(), -1);
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
