package com.alex.message.rmq.producer;

import com.alex.message.Launcher;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class MessageServiceTest extends Launcher {

    @Autowired
    @Qualifier("defaultRabbitMessageProducer")
    private RabbitMessageProducer rabbitMessageProducer;

    @Test
    public void testSend() {
        rabbitMessageProducer.send("rabbit_msg_test", "33333");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
