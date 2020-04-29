package com.alex.message.rmq.consumer;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

//@Component
public class RabbitMessageHandlerTest implements MessageHandler<Book> {

    @Override
    public void handleMessage(Book message) throws MessageException {
        System.out.println("RabbitMQ MessageHandler: " + JSON.toJSONString(message));
    }

    @Override
    public MessageListenerContainerConfig getMessageListenerContainerConfig() {
        MessageListenerContainerConfig cfg = new MessageListenerContainerConfig();
        cfg.setDestName("rabbit_msg_handler_test");
        return cfg;
    }

}
