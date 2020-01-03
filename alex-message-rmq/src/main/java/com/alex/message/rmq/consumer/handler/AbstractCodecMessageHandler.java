package com.alex.message.rmq.consumer.handler;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageCodecException;
import com.alex.message.exception.MessageException;
import com.alex.message.rmq.MessageInfo;
import com.alex.message.rmq.codec.Codec;
import com.alex.message.rmq.codec.CodecFactory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 消息监听抽象类
 */
public abstract class AbstractCodecMessageHandler<T> implements MessageHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCodecMessageHandler.class);

    private Class<T> clazz;

    public AbstractCodecMessageHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void handleMessage(MessageInfo messageInfo) throws MessageException {
        Long begin = System.currentTimeMillis();
        T msg = decode(messageInfo);
        LOGGER.info("The queue {} start to process message {} with header {}", messageInfo.getQueueName(), msg, messageInfo.getHeaders());
        handleMessage(msg);
        LOGGER.info("The queue {} process message finished, elapsed time is {}", messageInfo.getQueueName(), System.currentTimeMillis() - begin);
    }

    /**
     * 序列化对象
     */
    private T decode(MessageInfo messageInfo) throws MessageCodecException {
        T obj = null;
        Object contentType = messageInfo.getHeaders().get(MessageInfo.MESSAGE_CONTENT_TYPE);
        if (contentType != null && StringUtils.isNotBlank(contentType.toString())) {
            Codec codec = CodecFactory.getCodes(contentType.toString());
            obj = codec.decode(messageInfo.getBody(), clazz);
        }
        return obj;
    }

    @Override
    public MessageListenerContainerConfig getMessageListenerContainerConfig() {
        return null;
    }
}
