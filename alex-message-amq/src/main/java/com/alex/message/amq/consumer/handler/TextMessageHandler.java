package com.alex.message.amq.consumer.handler;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;

/**
 * 文本消息的基类,适用于接收文本类型消息并且需要手动签收的消息
 */
public abstract class TextMessageHandler implements MessageHandler<String> {

    /**
     * 处理消息的代码调用
     */
    @Override
    public abstract void handleMessage(String text) throws MessageException;

}
