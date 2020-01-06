package com.alex.message.rmq.producer;

import com.alex.message.exception.MessageException;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.MessageInfo;
import com.alex.message.rmq.MessageInfoBuilder;
import com.alex.message.rmq.codec.Codec;
import com.alex.message.rmq.codec.FastJsonCodec;
import org.apache.commons.lang.StringUtils;
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

    protected MessageInfo createMessage(String queueName, Object message) {
        MessageInfo msg;
        if (message == null) {
            throw new MessageException("message must not null!");
        }
        if (message instanceof MessageInfo) { // 直接传入MessageBase对象
            msg = MessageInfoBuilder.build((MessageInfo) message, queueName);
        } else {
            msg = MessageInfoBuilder.build(codec, message, queueName);
        }
        msg.setQueueName(queueName);
        setBrokerName(msg);
        return msg;
    }

    private void setBrokerName(MessageInfo messageInfo) {
        String brokerName = Broker.DEFAULT_BROKER_NAME;
        // 根据消息头中的灰度标记，如包含，设置消息发送到灰度队列中
        if (messageInfo.getHeaders().containsKey(Broker.GRAY_ENV)) {
            Object grayEnv = messageInfo.getHeaders().get(Broker.GRAY_ENV);
            // 消息头中含环境变更对象
            if (grayEnv != null && StringUtils.isNotBlank(grayEnv.toString())) {
                brokerName = brokerName + "_" + Broker.GRAY_BETA;
            }
        }
        LOGGER.info("message {} broker name is {}", messageInfo, brokerName);
        messageInfo.setBrokerName(brokerName);
    }
}
