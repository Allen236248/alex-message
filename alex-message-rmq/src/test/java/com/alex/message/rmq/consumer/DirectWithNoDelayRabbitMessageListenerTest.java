package com.alex.message.rmq.consumer;

import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;
import com.alibaba.fastjson.JSON;

@RabbitListenerAttribute(destName = "direct_no_delay_rabbit_test")
public class DirectWithNoDelayRabbitMessageListenerTest extends AbstractMessageListener<Book> {

    public DirectWithNoDelayRabbitMessageListenerTest() {
        super(Book.class);
    }

    @Override
    public void doHandle(Book msg) throws MessageException {
        System.out.println("RabbitMQ direct_no_delay_rabbit_test Listener: " + JSON.toJSONString(msg));
    }
}
