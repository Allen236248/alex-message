package com.alex.message.rmq.producer;

import com.alex.message.rmq.MessageInfo;
import com.alex.message.rmq.connection.RabbitConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 通用消息服务实现类
 */
@Component
public class DefaultRabbitMessageProducer extends AbstractRabbitMessageProducer implements RabbitMessageProducer {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultRabbitMessageProducer.class);

    @Autowired
    private RabbitConnectionManager rabbitConnectionManager;

    @Override
    public void send(String queueName, Object message) {
        MessageInfo msg = createMessage(queueName, message);
        send(queueName, msg, msg.getBrokerName());
        LOGGER.info("发送点对点消息：queueName:{}，源消息:{}，消息头:{}", queueName, message, msg);
    }

    @Override
    public void send(String queueName, Object message, long delayTime) {
        MessageInfo msg = createMessage(queueName, message);
        send(queueName, msg, msg.getBrokerName(), true, delayTime);
        LOGGER.info("发送点对点延时消息：queueName:{}，源消息:{}，消息头:{}", queueName, message, msg);
    }

    @Override
    public void publish(String topicName, Object message) {
        publish(topicName, message, false);
    }

    @Override
    public void publish(String topicName, Object message, boolean isPersistent) {
        MessageInfo msg = createMessage(topicName, message);
        publish(topicName, msg, msg.getBrokerName(), isPersistent);
        LOGGER.info("发送广播消息：queueName:{}，源消息:{}，消息头:{}", topicName, message, msg);
    }

    private void send(String queueName, Object message, String broker) {
        RabbitTemplate rabbitTemplate = rabbitConnectionManager.getRabbitTemplateForDirect(queueName, broker);
        rabbitTemplate.convertAndSend(queueName, message);
    }

    private void send(String queueName, Object message, String broker, boolean isDelay, final long delayTime) {
        RabbitTemplate rabbitTemplate = rabbitConnectionManager.getRabbitTemplateForDirect(queueName, isDelay, broker);
        rabbitTemplate.convertAndSend(queueName, message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                // 设置消息属性-过期时间
                message.getMessageProperties().setExpiration(String.valueOf(delayTime));
                return message;
            }
        });
    }

    private void publish(String topicName, Object message, String broker, boolean isPersistent) {
        RabbitTemplate amqpTemplate = rabbitConnectionManager.getRabbitTemplateForFanout(topicName, broker, isPersistent);
        amqpTemplate.convertAndSend(message);
    }

}
