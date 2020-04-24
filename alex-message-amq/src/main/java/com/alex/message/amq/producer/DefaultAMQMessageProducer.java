package com.alex.message.amq.producer;

import com.alibaba.fastjson.JSON;
import org.apache.activemq.command.ActiveMQDestination;
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
        send(destination, message);
    }

    @Override
    public void publish(final String topicName, final Object message) {
        Destination destination = new ActiveMQTopic(topicName);
        send(destination, message);
    }

    private void send(Destination destination, final Object message) {
        ActiveMQDestination physicalDestination = (ActiveMQDestination) destination;
        final String physicalName = physicalDestination.getPhysicalName();
        jmsTemplate.send(destination, new MessageCreator() {

            @Override
            public Message createMessage(Session session) throws JMSException {
                String text = JSON.toJSONString(message);
                LOGGER.info("发布JMS消息到{}:" + text, physicalName);
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
