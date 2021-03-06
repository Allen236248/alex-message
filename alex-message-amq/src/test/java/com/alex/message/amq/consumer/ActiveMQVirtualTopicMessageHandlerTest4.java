package com.alex.message.amq.consumer;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import org.springframework.stereotype.Component;

@Component
public class ActiveMQVirtualTopicMessageHandlerTest4 implements MessageHandler<String> {

    @Override
    public void handleMessage(String message) throws MessageException {
        System.out.println("ActiveMQ VirtualTopic MessageHandler B2 : " + message);
    }

    @Override
    public MessageListenerContainerConfig getMessageListenerContainerConfig() {
        MessageListenerContainerConfig cfg = new MessageListenerContainerConfig();
        cfg.setDestName("Consumer.B.VirtualTopic.amq_virtual_topic_msg_test");
        //如果是广播，需要设置为true
        //cfg.setPubSubDomain(true);
        //广播不支持多线程，即：concurrentConsumers=maxConcurrentConsumers=1
        return cfg;
    }
}
