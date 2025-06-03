package com.ljh.fawnhealth.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue fhQueue() {
        return QueueBuilder.durable(MqConstant.FH_QUEUE_NAME).build();
    }

    @Bean
    public Queue commentLikeQueue() {
        return QueueBuilder.durable(MqConstant.COMMENT_LIKE_QUEUE_NAME).build();
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(MqConstant.FH_EXCHANGE_NAME, true, false);
    }

    @Bean
    public Binding fhBinding() {
        return BindingBuilder.bind(fhQueue())
                .to(topicExchange())
                .with(MqConstant.FH_ROUTING_KEY);
    }

    @Bean
    public Binding commentLikeBinding() {
        return BindingBuilder.bind(commentLikeQueue())
                .to(topicExchange())
                .with(MqConstant.COMMENT_LIKE_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper());
    }

    // 开启自动声明（默认开启，但可以显示声明以确保）
    @Bean
    public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
