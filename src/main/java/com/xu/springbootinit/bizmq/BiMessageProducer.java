package com.xu.springbootinit.bizmq;

import com.xu.springbootinit.constant.CommonConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author: xuJing
 * @date: 2024/4/8 20:01
 */

@Component
public class BiMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * @param exchange 交换机名称
     * @param routingKey 路由键
     * @param message 消息
     */
    public void sendMessage(String message){
        rabbitTemplate.convertAndSend(CommonConstant.BI_EXCHANGE_NAME, CommonConstant.BI_ROUTING_KEY, message);
    }

}
