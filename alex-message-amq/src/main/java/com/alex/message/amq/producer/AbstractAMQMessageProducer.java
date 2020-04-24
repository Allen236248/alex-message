package com.alex.message.amq.producer;

import com.alex.message.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

public abstract class AbstractAMQMessageProducer implements AMQMessageProducer, MessageProducer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAMQMessageProducer.class);

    //虚拟主题
    protected static final String VIRTUAL_TOPIC_PREFIX = "VirtualTopic.";

    @Autowired
    protected JmsTemplate jmsTemplate;

}
