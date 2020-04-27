package com.alex.message.amq.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;

public abstract class AbstractActiveMQMessageProducer implements ActiveMQMessageProducer {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractActiveMQMessageProducer.class);

    //虚拟主题
    protected static final String VIRTUAL_TOPIC_PREFIX = "VirtualTopic.";

    @Autowired
    protected JmsTemplate jmsTemplate;

}
