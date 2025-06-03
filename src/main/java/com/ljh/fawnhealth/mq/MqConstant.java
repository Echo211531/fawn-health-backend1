package com.ljh.fawnhealth.mq;

public interface MqConstant {
    String FH_EXCHANGE_NAME = "fh_exchange";

    String FH_QUEUE_NAME = "fh_queue";

    String FH_ROUTING_KEY = "fh_routingKey";

    String FH_DEAD_ROUTING_KEY = "fh_dead_routingKey";

    // 新增评论点赞路由键
    String COMMENT_LIKE_ROUTING_KEY = "comment.like";

    String COMMENT_LIKE_QUEUE_NAME = "comment_like_queue";
}
