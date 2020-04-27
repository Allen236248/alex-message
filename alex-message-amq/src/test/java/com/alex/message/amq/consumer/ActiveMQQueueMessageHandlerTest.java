package com.alex.message.amq.consumer;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import org.springframework.stereotype.Component;

@Component
public class ActiveMQQueueMessageHandlerTest implements MessageHandler<String> {

    @Override
    public void handleMessage(String message) throws MessageException {
        System.out.println("ActiveMQ Queue MessageHandler: " + message);
    }

    @Override
    public MessageListenerContainerConfig getMessageListenerContainerConfig() {
        MessageListenerContainerConfig cfg = new MessageListenerContainerConfig();
        cfg.setDestName("amq_queue_msg_test");
        return cfg;
    }
}
