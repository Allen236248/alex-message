package com.allen.message.producer;

import com.allen.message.amq.producer.AbstractAMQMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessageProducer implements MessageProducer {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractAMQMessageProducer.class);

}
