package com.allen.message.rmq.service;

import com.alex.message.utils.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ消息发送实现类
 *
 * @author Frank 平台架构部
 * @date 2017年7月10日
 * @version V3.0.0
 */
public class RabbitMessageService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private RabbitConnectionManager rabbitConnectionManager;

    public RabbitConnectionManager getRabbitConnectionManager() {
        if (rabbitConnectionManager == null) {
            rabbitConnectionManager = SpringContextHolder.getBean(RabbitConnectionManager.class);
        }
        return rabbitConnectionManager;
    }

    public void send(String queueName, Object message, String broker) {
        RabbitTemplate amqpTemplate = this.getRabbitConnectionManager().getTemplate(broker);
        String queue = this.getRabbitConnectionManager().getQueueName(queueName, broker);
        amqpTemplate.convertAndSend(queue, message);
    }

    public void send(String queueName, Object message, boolean isPersistent, String borker) {
        RabbitTemplate amqpTemplate = this.getRabbitConnectionManager().getTemplate(borker);
        String queue = this.getRabbitConnectionManager().getQueueName(queueName, borker);
        amqpTemplate.convertAndSend(queue, message);
    }

    public void publish(String topicName, Object message, String broker) {
        RabbitTemplate amqpTemplate = this.getRabbitConnectionManager().getTemplate(topicName, broker);
        amqpTemplate.convertAndSend(message);
    }

    public void persistentPublish(String topicName, Object message, String broker) {
        RabbitTemplate amqpTemplate = this.getRabbitConnectionManager().getPersistentPublishTemplate(topicName, broker);
        amqpTemplate.convertAndSend(message);
    }

    public void send(String queueName, Object message, String broker, boolean deadLetter, final long delayTime) {
        RabbitTemplate amqpTemplate = this.getRabbitConnectionManager().getTemplate(broker);
        String queue = this.getRabbitConnectionManager().getQueueName(queueName, broker, deadLetter);
        amqpTemplate.convertAndSend(queue, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置消息属性-过期时间
                message.getMessageProperties().setExpiration(String.valueOf(delayTime));
                return message;
            }
        });
    }

}
