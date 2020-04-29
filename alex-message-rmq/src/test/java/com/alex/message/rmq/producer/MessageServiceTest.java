package com.alex.message.rmq.producer;

import com.alex.message.Launcher;
import com.alex.message.model.Book;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Calendar;

public class MessageServiceTest extends Launcher {

    @Autowired
    @Qualifier("defaultRabbitMessageProducer")
    private RabbitMessageProducer rabbitMessageProducer;

    @Test
    public void testSendWithHandlerConsumer() {
        rabbitMessageProducer.send("rabbit_msg_handler_test", "333333");

        threadJoin();
    }

    @Test
    public void testDirectWithNoDelayRabbitMessageListener() {
        Book book = createBook("listener");
        rabbitMessageProducer.send("direct_no_delay_rabbit_test", book);

        threadJoin();
    }

    @Test
    public void testDirectWithDelayRabbitMessageListener() {
        Book book = createBook("listener");
        rabbitMessageProducer.send("direct_delay_rabbit_test", book, 10 * 1000);

        threadJoin();
    }

    @Test
    public void testFanoutRabbitMessageListener() {
        Book book = createBook("listener");
        for(int i = 0; i < 5; i++)
            rabbitMessageProducer.publish("fanout_rabbit_test", book);

        threadJoin();
    }

    private static Book createBook(String mark) {
        Book book = new Book();
        book.setName("追风筝的人" + mark);
        book.setAuthName("卡勒德·胡赛尼");
        Calendar calendar = Calendar.getInstance();
        calendar.set(2003, 5, 1);
        book.setPublishDate(calendar.getTime());
        return book;
    }

    private static void threadJoin() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
