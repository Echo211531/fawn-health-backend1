package com.ljh.fawnhealth.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.AllowedListDeserializingMessageConverter;

@Configuration
public class RabbitConfig {

//    // 声明队列，自动创建
//    @Bean
//    public Queue fhQueue() {
//        return new Queue(MqConstant.FH_QUEUE_NAME, true, false, false);  // 使用 MqConstant 中的队列名称
//    }
//
//    // 声明交换机，自动创建
//    @Bean
//    public Exchange directExchange() {
//        return new DirectExchange(MqConstant.FH_EXCHANGE_NAME, true, false);  // 使用 MqConstant 中的交换机名称
//    }
//
//    // 绑定队列到交换机
//    @Bean
//    public Binding binding() {
//        return BindingBuilder.bind(fhQueue()).to(directExchange()).with(MqConstant.FH_ROUTING_KEY).noargs();  // 使用 MqConstant 中的路由键
//    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        return new Jackson2JsonMessageConverter(mapper);
    }
}
