package com.zr.health.mq;

public interface MqConstant {
    // ====================== 主队列配置 ======================
    // 主交换机（Topic类型）
    String FH_EXCHANGE_NAME = "fh.exchange";

    // 优惠券主队列
    String FH_QUEUE_NAME = "fh.queue.coupon";
    String FH_ROUTING_KEY = "fh.key.coupon"; // 优惠券路由键

    // 点赞主队列
    String COMMENT_LIKE_QUEUE_NAME = "fh.queue.like";
    String COMMENT_LIKE_ROUTING_KEY = "fh.key.like"; // 点赞路由键

    // ====================== 死信队列配置 ======================
    // 死信交换机（Direct类型）
    String DEAD_LETTER_EXCHANGE = "fh.dlx.exchange";

    // 优惠券死信队列
    String FH_DLQ_QUEUE_NAME = "fh.dlx.queue.coupon";
    String FH_DLQ_ROUTING_KEY = "fh.dlx.key.coupon"; // 优惠券死信路由键

    // 点赞死信队列
    String LIKE_DLQ_QUEUE_NAME = "fh.dlx.queue.like";
    String LIKE_DLQ_ROUTING_KEY = "fh.dlx.key.like"; // 点赞死信路由键

    // ====================== 队列参数 ======================
    int MESSAGE_TTL = 30000; // 消息过期时间（30秒）
    boolean QUEUE_DURABLE = true; // 队列持久化
}