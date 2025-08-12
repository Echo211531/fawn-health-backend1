//package com.ljh.fawnhealth.config;
//
//import org.springframework.amqp.core.*;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
////@Configuration
//public class RabbitMQConfig {
//
//    /**
//     * 正常队列
//     */
//    public static final String EXCHANGE = "bi_exchange";
//
//    public static final String QUEUE = "lm_queue";
//
//    public static final String ROUTING_KEY = "boot-rout";
//
//    /**
//     * 死信队列
//     */
//    public static final String DEAD_EXCHANGE = "lm_dead_exchange";
//
//    public static final String DEAD_QUEUE = "lm_dead_queue";
//
//    public static final String DEAD_ROUTING_KEY = "dead-rout";
//
//    /**
//     * 声明死信交换机
//     *
//     * @return
//     */
//    @Bean
//    public Exchange deadExchange() {
//        return ExchangeBuilder.directExchange(DEAD_EXCHANGE).build();
//    }
//
//    /**
//     * 声明死信队列
//     *
//     * @return
//     */
//    @Bean
//    public Queue deadQueue() {
//        return QueueBuilder.durable(DEAD_QUEUE).build();
//    }
//
//
//    /**
//     * 绑定死信的队列和交换机
//     *
//     * @param deadExchange
//     * @param deadQueue
//     * @return
//     */
//    @Bean
//    public Binding deadBind(Exchange deadExchange, Queue deadQueue) {
//        return BindingBuilder.bind(deadQueue).to(deadExchange).with(DEAD_ROUTING_KEY).noargs();
//    }
//
//    /**
//     * 声明交换机，同channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT);
//     *
//     * @return
//     */
//    @Bean
//    public Exchange bootExchange() {
//        return ExchangeBuilder.directExchange(EXCHANGE).build();
//    }
//
//    /**
//     * 声明队列，同channel.queueDeclare(QUEUE, true, false, false, null);
//     * 绑定死信交换机及路由key
//     *
//     * @return
//     */
//    @Bean
//    public Queue bootQueue() {
//        return QueueBuilder.durable(QUEUE)
//                .deadLetterExchange(DEAD_EXCHANGE)
//                .deadLetterRoutingKey(DEAD_ROUTING_KEY)
//                //声明队列属性有更改时需要删除队列
//                //给队列设置消息时长
//                //.ttl(10000)
//                //队列最大长度
//                .maxLength(1)
//                .build();
//    }
//
//    /**
//     * 绑定队列和交换机,同 channel.queueBind(QUEUE, EXCHANGE, ROUTING_KEY);
//     *
//     * @param bootExchange
//     * @param bootQueue
//     * @return
//     */
//    @Bean
//    public Binding bootBind(Exchange bootExchange, Queue bootQueue) {
//        return BindingBuilder.bind(bootQueue).to(bootExchange).with(ROUTING_KEY).noargs();
//    }
//}
//
