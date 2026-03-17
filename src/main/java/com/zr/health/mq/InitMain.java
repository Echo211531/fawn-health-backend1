//package com.ljh.fawnhealth.mq;
//
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
// */
//public class InitMain {
//    public static void main(String[] args) {
//        try {
//            ConnectionFactory factory = new ConnectionFactory();
//            factory.setHost("localhost");
//            Connection connection = factory.newConnection();
//            Channel channel = connection.createChannel();
//            String EXCHANGE_NAME =  MqConstant.FH_EXCHANGE_NAME;
//            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
//
//            // 创建队列，随机分配一个队列名称
//            String queueName = MqConstant.FH_QUEUE_NAME;
//            channel.queueDeclare(queueName, true, false, false, null);
//            channel.queueBind(queueName, EXCHANGE_NAME,  MqConstant.FH_ROUTING_KEY);
//
//            // 创建死信交换机
//            String DEAD_LETTER_EXCHANGE = "lm_dead_exchange";
//            channel.exchangeDeclare(DEAD_LETTER_EXCHANGE, "direct");
//
//            // 创建死信队列
//            String DEAD_LETTER_QUEUE = "lm_dead_queue";
//            channel.queueDeclare(DEAD_LETTER_QUEUE, true, false, false, null);
//
//            // 绑定死信队列到死信交换机
//            channel.queueBind(DEAD_LETTER_QUEUE, DEAD_LETTER_EXCHANGE, MqConstant.FH_DEAD_ROUTING_KEY);
//
//            // 设置原队列的死信属性
//            Map<String, Object> argsMap = new HashMap<>();
//            argsMap.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
//            argsMap.put("x-dead-letter-routing-key", MqConstant.FH_DEAD_ROUTING_KEY);
//            channel.queueDeclare(queueName, true, false, false, argsMap);
//
//            System.out.println("Setup completed successfully.");
//
//        } catch (Exception e) {
//
//        }
//
//    }
//}
