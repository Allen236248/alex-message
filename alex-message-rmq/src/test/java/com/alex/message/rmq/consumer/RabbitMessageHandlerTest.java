package com.alex.message.rmq.consumer;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.model.Book;
import com.alex.message.utils.ReflectionUtils;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Component
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

    public static void main(String...args) {
        MessageHandler<Book> messageHandler = new RabbitMessageHandlerTest();
        System.out.println(messageHandler.getClass().getName());
        Type[] types = messageHandler.getClass().getGenericInterfaces();
        for(Type type : types) {
            if(!(type instanceof ParameterizedType))
                continue;
            ParameterizedType t = (ParameterizedType) type;
            String typeName = t.getRawType().getTypeName();
            if(MessageHandler.class.getName().equals(typeName)) {
                System.out.println((Class) t.getActualTypeArguments()[0]);
            }
        }
    }
}
