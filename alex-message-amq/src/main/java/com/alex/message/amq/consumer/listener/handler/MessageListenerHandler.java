package com.alex.message.amq.consumer.listener.handler;

import com.alex.message.consumer.handler.MessageHandler;

import javax.jms.Message;

public interface MessageListenerHandler<T, M extends Message> {

    void handleMessage(M message) throws Exception;

    T convert(M message, Class<T> clazz) throws Exception;

    void setMessageHandler(MessageHandler<T> messageHandler);

}
