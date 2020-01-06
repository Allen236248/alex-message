package com.allen.message.amq.producer;

import com.allen.message.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;

public abstract class AbstractAMQMessageProducer implements AMQMessageProducer, MessageProducer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractAMQMessageProducer.class);

    //虚拟主题
    protected static final String VIRTUAL_TOPIC_PREFIX = "VirtualTopic.";

    @Autowired
    protected JmsTemplate jmsTemplate;

    @Autowired
    @Qualifier("jmsConnectionFactory")
    protected ConnectionFactory connectionFactory;

}
