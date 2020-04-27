package com.alex.message.rmq.consumer.listener;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.utils.ReflectionUtils;

/**
 * 消息处理代码类
 */
public final class MessageListenerDelegate<T> extends AbstractMessageListener<T> {

    private MessageHandler<T> messageHandler;

    public MessageListenerDelegate(MessageHandler<T> messageHandler) {
        super(ReflectionUtils.getActualType(messageHandler, MessageHandler.class));
        this.messageHandler = messageHandler;
    }

    @Override
    public void doHandle(T msg) throws MessageException {
        this.messageHandler.handleMessage(msg);
    }

    public MessageHandler<T> getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }

}
