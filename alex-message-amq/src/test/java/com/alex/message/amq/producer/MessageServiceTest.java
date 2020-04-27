package com.alex.message.amq.producer;

import com.alex.message.Launcher;
import com.alex.message.producer.MessageProducer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class MessageServiceTest extends Launcher {

    @Autowired
    @Qualifier("defaultActiveMQMessageProducer")
    private ActiveMQMessageProducer activeMQMessageProducer;

    @Test
    public void testSend() {
        //队列模式，发送10条消息，多个消费者轮流接收
        for(int i = 0; i < 10; i++) {
            activeMQMessageProducer.send("amq_queue_msg_test", "111111_" + i);
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPublish() {
        //广播模式，发送10条消息，多个消费者同时接收
        for(int i = 0; i < 10; i++) {
            activeMQMessageProducer.publish("amq_topic_msg_test", "222222_" + i);
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVirtualPublish() {
        // 在分布式集群环境下，相同的应用多个模块，保证每个模块轮询消费。
        // Virtual Topic可以理解为是Queue和Topic的结合，即：使用Topic的一对多的广播功能，又需要在集群的时候，只有一个收到，也就是队列的一对一的特效。
        // 队列名称为A的两个消费者轮流接收消息，队列名称为B的两个消费者轮流接收消息，但是发送的消息，A和B都会接收
        for(int i = 0; i < 10; i++) {
            activeMQMessageProducer.virtualPublish("amq_virtual_topic_msg_test", "333333_" + i);
        }
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
