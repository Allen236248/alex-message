package com.alex.message.rmq.consumer;

import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;
import com.alibaba.fastjson.JSON;

@RabbitListenerAttribute(destName = "fanout_rabbit_test", isPublish = true, consumerId="FanoutRabbitMessageListenerTest", concurrentConsumers = 1)
public class FanoutRabbitMessageListenerTest2 extends AbstractMessageListener<Book> {

    public FanoutRabbitMessageListenerTest2() {
        super(Book.class);
    }

    @Override
    public void doHandle(Book msg) throws MessageException {
        System.out.println("RabbitMQ fanout_rabbit_test Listener 2 : " + JSON.toJSONString(msg));
    }
}
