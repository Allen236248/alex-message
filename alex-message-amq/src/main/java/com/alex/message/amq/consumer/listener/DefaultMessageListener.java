package com.alex.message.amq.consumer.listener;

import com.alex.message.amq.consumer.listener.handler.MessageListenerHandler;
import com.alex.message.amq.consumer.listener.handler.TextMessageListenerHandler;
import com.alex.message.exception.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * 消息消费监听器
 */
public class DefaultMessageListener implements MessageListener {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageListener.class);

    private MessageListenerHandler messageListenerHandler = new TextMessageListenerHandler();

    public DefaultMessageListener() {
    }

    @Override
    public final void onMessage(Message message) {
        try {
            messageListenerHandler.handleMessage(message);
        } catch (Exception e) {
            LOGGER.error("JMS异常！", e);
            throw new MessageException(e);
        }
    }

    public MessageListenerHandler getMessageListenerHandler() {
        return messageListenerHandler;
    }

    public void setMessageListenerHandler(MessageListenerHandler messageListenerHandler) {
        this.messageListenerHandler = messageListenerHandler;
    }

}
