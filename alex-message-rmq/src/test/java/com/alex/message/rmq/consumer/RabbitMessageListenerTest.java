package com.alex.message.rmq.consumer;

import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;
import com.alibaba.fastjson.JSON;

//@RabbitListenerAttribute(destName = "rabbit_msg_test_delay", isDeadLetter = true)
@RabbitListenerAttribute(destName = "rabbit_msg_test")
public class RabbitMessageListenerTest extends AbstractMessageListener<Book> {

    public RabbitMessageListenerTest() {
        super(Book.class);
    }

    @Override
    public void doHandle(Book msg) throws MessageException {
        System.out.println("RabbitMQ Listener: " + JSON.toJSONString(msg));
    }
}
