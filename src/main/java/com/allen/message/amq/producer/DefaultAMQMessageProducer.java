package com.allen.message.amq.producer;

import com.alibaba.fastjson.JSON;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.jms.*;

/**
 * 默认的消息服务实现，依赖spring
 */
@Service
public class DefaultAMQMessageProducer extends AbstractAMQMessageProducer {

    @Override
    public void send(final String queueName, final Object message) {
        Destination destination = new ActiveMQQueue(queueName);
        jmsTemplate.send(destination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                String text = JSON.toJSONString(message);
                LOGGER.info("发送JMS消息到队列{}:" + text, queueName);
                return session.createTextMessage(text);
            }
        });
    }

    @Override
    public void publish(final String topicName, final Object message) {
        Destination destination = new ActiveMQTopic(topicName);
        jmsTemplate.send(destination, new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                String text = JSON.toJSONString(message);
                LOGGER.info("发布JMS广播消息到主题:" + text, topicName);
                return session.createTextMessage(text);
            }
        });
    }


    @Override
    public void virtualPublish(String topicName, final Object message) {
        if (!topicName.startsWith(VIRTUAL_TOPIC_PREFIX)) {
            topicName = VIRTUAL_TOPIC_PREFIX + topicName;
        }
        publish(topicName, message);
    }

}
