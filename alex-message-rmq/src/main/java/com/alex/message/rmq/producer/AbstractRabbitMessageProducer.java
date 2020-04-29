package com.alex.message.rmq.producer;

import com.alex.message.exception.MessageException;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.MessageInfo;
import com.alex.message.rmq.MessageInfoBuilder;
import com.alex.message.rmq.codec.Codec;
import com.alex.message.rmq.codec.FastJsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public abstract class AbstractRabbitMessageProducer implements RabbitMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRabbitMessageProducer.class);

    // 默认的序列化转换器
    private Codec codec;

    @PostConstruct
    public void initCode() {
        codec = new FastJsonCodec();
    }

    protected MessageInfo createMessage(String destName, Object message) {
        MessageInfo msg;
        if (message == null) {
            throw new MessageException("message must not null!");
        }
        if (message instanceof MessageInfo) { // 直接传入MessageBase对象
            msg = MessageInfoBuilder.build((MessageInfo) message);
        } else {
            msg = MessageInfoBuilder.build(codec, message);
        }
        msg.setDestName(destName);
        setBrokerName(msg);
        return msg;
    }

    private void setBrokerName(MessageInfo messageInfo) {
        String brokerName = Broker.DEFAULT_BROKER_NAME;
        LOGGER.info("message {} broker name is {}", messageInfo, brokerName);
        messageInfo.setBrokerName(brokerName);
    }
}
