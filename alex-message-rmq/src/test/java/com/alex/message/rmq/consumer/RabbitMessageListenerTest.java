package com.alex.message.rmq.consumer;

import com.alex.message.exception.MessageException;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;

//@RabbitListenerAttribute(destName = "rabbit_msg_test_delay", isDeadLetter = true)
@RabbitListenerAttribute(destName = "rabbit_msg_test")
public class RabbitMessageListenerTest extends AbstractMessageListener<String> {

    public RabbitMessageListenerTest() {
        super(String.class);
    }

    @Override
    public void doHandle(String msg) throws MessageException {
        System.out.println("RabbitMQ: " + msg);
    }
}
