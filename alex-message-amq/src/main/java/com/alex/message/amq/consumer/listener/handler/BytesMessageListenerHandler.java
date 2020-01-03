package com.alex.message.amq.consumer.listener.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.BytesMessage;

public class BytesMessageListenerHandler<T> extends AbstractMessageListenerHandler<T, BytesMessage> {

    private final Logger LOGGER = LoggerFactory.getLogger(BytesMessageListenerHandler.class);

    @Override
    public T convert(BytesMessage message, Class<T> clazz) throws Exception {
        return null;
    }
}
