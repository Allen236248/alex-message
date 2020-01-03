package com.alex.message.amq.consumer.listener.handler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.TextMessage;

public class TextMessageListenerHandler<T> extends AbstractMessageListenerHandler<T, TextMessage> {

    private final Logger LOGGER = LoggerFactory.getLogger(TextMessageListenerHandler.class);

    @Override
    public T convert(TextMessage message, Class<T> clazz) throws Exception {
        String text = message.getText();
        // JSON字符串转换为对象
        return JSON.parseObject(text, clazz);
    }
}
