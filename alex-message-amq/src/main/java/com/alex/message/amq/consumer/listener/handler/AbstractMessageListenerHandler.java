package com.alex.message.amq.consumer.listener.handler;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;

public abstract class AbstractMessageListenerHandler<T, M extends Message> implements MessageListenerHandler<T, M> {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageListenerHandler.class);

    private MessageHandler<T> messageHandler;

    @Override
    public void handleMessage(M message) throws Exception {
        Class<T> clazz = ReflectionUtils.getActualType(messageHandler);
        T msg = convert(message, clazz);
        messageHandler.handleMessage(msg);
        message.acknowledge();
    }

    public MessageHandler<T> getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }
}
