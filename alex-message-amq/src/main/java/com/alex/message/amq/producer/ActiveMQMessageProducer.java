package com.alex.message.amq.producer;

import com.alex.message.producer.MessageProducer;

public interface ActiveMQMessageProducer extends MessageProducer {

    /**
     * 虚拟广播，向指定通道发布一条虚拟主题
     *
     * Broker接收到广播消息后会自动为每一个持久化订阅者创建一个持久化的队列用来存放广播消息，每一个持久化订阅者可最多一次成功消费该广播消息
     */
    void virtualPublish(String topicName, Object message);

}
