package com.zr.health.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class RabbitMQConfig {

    // ====================== 主队列声明 ======================
    // 优惠券主队列（绑定死信交换机和TTL）
    @Bean
    public Queue couponQueue() {
        return QueueBuilder.durable(MqConstant.FH_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", MqConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstant.FH_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", MqConstant.MESSAGE_TTL)
                .build();
    }

    // 点赞主队列（绑定死信交换机和TTL）
    @Bean
    public Queue likeQueue() {
        return QueueBuilder.durable(MqConstant.COMMENT_LIKE_QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", MqConstant.DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstant.LIKE_DLQ_ROUTING_KEY)
                .withArgument("x-message-ttl", MqConstant.MESSAGE_TTL)
                .build();
    }

    // 主交换机（Topic类型）
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(MqConstant.FH_EXCHANGE_NAME, MqConstant.QUEUE_DURABLE, false);
    }

    // ====================== 死信队列声明 ======================
    // 优惠券死信队列
    @Bean
    public Queue couponDlq() {
        return new Queue(MqConstant.FH_DLQ_QUEUE_NAME, MqConstant.QUEUE_DURABLE);
    }

    // 点赞死信队列
    @Bean
    public Queue likeDlq() {
        return new Queue(MqConstant.LIKE_DLQ_QUEUE_NAME, MqConstant.QUEUE_DURABLE);
    }

    // 死信交换机（Direct类型）
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(MqConstant.DEAD_LETTER_EXCHANGE, MqConstant.QUEUE_DURABLE, false);
    }

    // ====================== 绑定关系 ======================
    // 优惠券主队列绑定到主交换机
    @Bean
    public Binding couponBinding() {
        return BindingBuilder.bind(couponQueue())
                .to(topicExchange())
                .with(MqConstant.FH_ROUTING_KEY);
    }

    // 点赞主队列绑定到主交换机
    @Bean
    public Binding likeBinding() {
        return BindingBuilder.bind(likeQueue())
                .to(topicExchange())
                .with(MqConstant.COMMENT_LIKE_ROUTING_KEY);
    }

    // 优惠券死信队列绑定到死信交换机
    @Bean
    public Binding couponDlqBinding() {
        return BindingBuilder.bind(couponDlq())
                .to(deadLetterExchange())
                .with(MqConstant.FH_DLQ_ROUTING_KEY);
    }

    // 点赞死信队列绑定到死信交换机
    @Bean
    public Binding likeDlqBinding() {
        return BindingBuilder.bind(likeDlq())
                .to(deadLetterExchange())
                .with(MqConstant.LIKE_DLQ_ROUTING_KEY);
    }

    // ====================== 其他配置 ======================
    // JSON 消息转换器
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 启用自动声明队列和交换机
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter // 注入 JSON 转换器
    ) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);

        // 1. 设置 JSON 消息转换器
        template.setMessageConverter(jsonMessageConverter);

        // 2. 配置生产者确认回调（用于调试消息发送结果）
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息已成功发送到交换机，相关数据: {}", correlationData);
            } else {
                log.error("消息发送到交换机失败，原因: {}, 相关数据: {}", cause, correlationData);
                // 这里可以添加重试逻辑或告警通知
            }
        });


        return template;
    }


}