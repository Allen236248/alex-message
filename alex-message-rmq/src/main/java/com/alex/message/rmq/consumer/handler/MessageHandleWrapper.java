package com.alex.message.rmq.consumer.handler;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.utils.ReflectUtil;

/**
 * 消息处理代码类
 */
public class MessageHandleWrapper<T> extends AbstractCodecMessageHandler<T> {

    private MessageHandler<T> messageHandler;

    public MessageHandleWrapper(MessageHandler<T> messageHandler) {
        super(ReflectUtil.getActualType(messageHandler));
        this.messageHandler = messageHandler;
    }

    @Override
    public void handleMessage(T msg) throws MessageException {
        this.messageHandler.handleMessage(msg);
    }

    public MessageHandler<T> getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }

}
