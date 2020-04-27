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
        activeMQMessageProducer.send("amq_queue_msg_test", "111111");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testPublish() {
        activeMQMessageProducer.publish("amq_topic_msg_test", "222222");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testVirtualPublish() {
        activeMQMessageProducer.virtualPublish("amq_virtual_topic_msg_test", "333333");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
