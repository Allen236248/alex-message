package com.allen.message.rmq.producer;

import com.allen.message.rmq.MessageInfo;
import com.allen.message.rmq.service.RabbitMessageService;
import com.allen.message.utils.NamedThreadFactory;
import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 通用消息服务实现类
 */
@Component
public class DefaultRabbitMessageProducer extends AbstractRabbitMessageProducer implements RabbitMessageProducer {

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultRabbitMessageProducer.class);

    // 批次大小
    private static int DEFAULT_SIZE = 10000;

    private static RabbitMessageService rabbitMessageService = new RabbitMessageService();

    private ExecutorService threadPool = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(512),
                    new NamedThreadFactory("RabbitMQMessageSendingThread"), new ThreadPoolExecutor.CallerRunsPolicy());

    @Override
    public void send(String queueName, Object message) {
        MessageInfo msg = createMessage(queueName, message);
        rabbitMessageService.send(queueName, msg, msg.getBrokerName());
        LOGGER.info("Send queue message to queuename:{},Original message:{}, message header:{}", queueName, message, msg);
    }

    @Override
    public void send(String queueName, Object message, boolean isPersistent) {
        MessageInfo msg = createMessage(queueName, message);
        rabbitMessageService.send(queueName, msg, isPersistent, msg.getBrokerName());
        LOGGER.info("Send queue message to queuename:{},Original message:{}, message header:{}", queueName, message, msg);
    }

    @Override
    public void send(String queueName, Object message, long delayTime) {
        MessageInfo msg = createMessage(queueName, message);
        rabbitMessageService.send(queueName, msg, msg.getBrokerName(), true, delayTime);
        LOGGER.info("Send queue message to queuename:{},Original message:{}, message header:{}", queueName, message, msg);
    }

    @Override
    public void publish(String topicName, Object message) {
        MessageInfo msg = createMessage(topicName, message);
        rabbitMessageService.publish(topicName, msg, msg.getBrokerName());
        LOGGER.info("Send publish message to topicName:{},Original message:{}, message header:{}", topicName, message, msg);
    }

    @Override
    public void publish(String topicName, Object message, boolean isPersistent) {
        MessageInfo msg = createMessage(topicName, message);
        rabbitMessageService.persistentPublish(topicName, msg, msg.getBrokerName());
        LOGGER.info("Send persistent Publish message to topicName:{},Original message:{}, message header:{}", topicName, message, msg);
    }

}
