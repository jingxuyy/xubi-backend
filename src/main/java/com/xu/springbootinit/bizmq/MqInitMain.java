package com.xu.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * @author: xuJing
 * @date: 2024/4/8 20:09
 */

public class MqInitMain {
    public static void main(String[] args) {
        try {
            // 创建链接工厂
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            // 创建连接
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 定义交换机名称
            String EXCHANGE_NAME = "code_exchange";

            // 声明交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 创建队列
            String queueName = "code_queue";

            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, EXCHANGE_NAME, "my_routingKey");
            System.out.println("=========");

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
