package com.alex.message.amq.consumer.listener;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.utils.ReflectionUtils;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * 消息消费监听器
 */
public class MessageListenerDelegate<T> implements MessageListener {

    private final Logger LOGGER = LoggerFactory.getLogger(MessageListenerDelegate.class);

    private MessageHandler<T> messageHandler;

    public MessageListenerDelegate() {
    }

    @Override
    public final void onMessage(Message message) {
        try {
            Class<T> clazz = ReflectionUtils.getActualType(messageHandler, MessageHandler.class);

            if(!(message instanceof TextMessage)) {
                throw new MessageException("不支持的消息类型");
            }
            TextMessage textMessage = (TextMessage)message;
            String text = textMessage.getText();
            // JSON字符串转换为对象
            T msg = JSON.parseObject(text, clazz);
            messageHandler.handleMessage(msg);
            message.acknowledge();
        } catch (Exception e) {
            LOGGER.error("JMS异常！", e);
            throw new MessageException(e);
        }
    }

    public MessageHandler<T> getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler<T> messageHandler) {
        this.messageHandler = messageHandler;
    }

}
