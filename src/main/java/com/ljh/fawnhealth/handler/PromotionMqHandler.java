package com.ljh.fawnhealth.handler;

import com.ljh.fawnhealth.mapper.CommentLikesMapper;
import com.ljh.fawnhealth.mapper.CouponsMapper;
import com.ljh.fawnhealth.model.dto.comments.LikeEventDTO;
import com.ljh.fawnhealth.model.dto.coupons.UserCouponDTO;
import com.ljh.fawnhealth.model.entity.Coupons;
import com.ljh.fawnhealth.mq.MqConstant;
import com.ljh.fawnhealth.service.CommentsService;
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

    @Resource
    private CommentLikesMapper commentLikesMapper;

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

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MqConstant.COMMENT_LIKE_QUEUE_NAME, durable = "true"),
            exchange = @Exchange(name = MqConstant.FH_EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = MqConstant.COMMENT_LIKE_ROUTING_KEY
    ))
    public void listenLikeMessage(LikeEventDTO likeEvent) {
        try {
            Long commentId = likeEvent.getCommentId();
            Long userId = likeEvent.getUserId();
            Boolean liked = likeEvent.getLiked();

            if (liked != null) {
                if (liked) {
                    // 点赞：插入或更新为未删除状态
                    commentLikesMapper.upsertLike(commentId, userId);
                } else {
                    // 取消点赞：逻辑删除
                    commentLikesMapper.markDeleted(commentId, userId);
                }
            }

            System.out.println("处理点赞消息（落库）: " + likeEvent);
        } catch (Exception e) {
            System.err.println("处理点赞消息异常: " + e.getMessage());
        }
    }



}