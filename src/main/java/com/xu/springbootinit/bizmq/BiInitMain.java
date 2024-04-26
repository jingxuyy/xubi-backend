package com.xu.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.xu.springbootinit.constant.CommonConstant;

/**
 * @author: xuJing
 * @date: 2024/4/8 20:09
 */

public class BiInitMain {
    public static void main(String[] args) {
        try {
            // 创建链接工厂
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");

            // 创建连接
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 定义交换机名称
            String EXCHANGE_NAME = CommonConstant.BI_EXCHANGE_NAME;

            // 声明交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");

            // 创建队列
            String queueName = CommonConstant.BI_QUEUE_NAME;

            channel.queueDeclare(queueName, true, false, false, null);

            channel.queueBind(queueName, EXCHANGE_NAME, CommonConstant.BI_ROUTING_KEY);

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
