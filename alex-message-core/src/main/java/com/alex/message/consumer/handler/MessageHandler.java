package com.alex.message.consumer.handler;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.exception.MessageException;

/**
 * 消息处理器
 */
public interface MessageHandler<T> {

    /**
     * 处理消息
     */
    void handleMessage(T message) throws MessageException;

    MessageListenerContainerConfig getMessageListenerContainerConfig();

}
