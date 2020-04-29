package com.alex.message.producer;

/**
 * 消息服务接口
 */
public interface MessageProducer {

    /**
     * 向队列发送消息
     */
    void send(String queueName, Object message);

    /**
     * 向指定主题广播消息
     */
    void publish(String topicName, Object message);

}
