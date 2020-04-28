package com.alex.message.rmq.consumer;

import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;
import com.alibaba.fastjson.JSON;

@RabbitListenerAttribute(destName = "direct_delay_rabbit_msg_test", isDeadLetter = true)
public class DirectWithDelayRabbitMessageListenerTest extends AbstractMessageListener<Book> {

    public DirectWithDelayRabbitMessageListenerTest() {
        super(Book.class);
    }

    @Override
    public void doHandle(Book msg) throws MessageException {
        System.out.println("RabbitMQ direct_delay_rabbit_msg_test Listener: " + JSON.toJSONString(msg));
    }
}
