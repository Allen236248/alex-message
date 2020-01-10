package com.alex.message.rmq.consumer.listener;

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
public abstract class AbstractMessageListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageListener.class);

    private Class<T> clazz;

    public AbstractMessageListener(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void handleMessage(MessageInfo messageInfo) throws MessageException {
        Long begin = System.currentTimeMillis();
        T msg = decode(messageInfo);
        LOGGER.info("The queue {} start to process message {} with header {}", messageInfo.getQueueName(), msg, messageInfo.getHeaders());
        doHandle(msg);
        LOGGER.info("The queue {} process message finished, elapsed time is {}", messageInfo.getQueueName(), System.currentTimeMillis() - begin);
    }

    public abstract void doHandle(T msg) throws MessageException;

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
        if (obj == null) {
            throw new MessageCodecException("Failed to transCodec Message content");
        }
        return obj;
    }

}
