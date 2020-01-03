package com.alex.message.rmq.converter;

import com.alex.message.rmq.MessageInfo;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.util.Map;
import java.util.UUID;

/**
 * 自定义消息转换工厂
 */
public class RabbitMessageConverter extends AbstractMessageConverter {

    /** 默认编码 */
    public static final String DEFAULT_CHARSET = "UTF-8";
    /** 消息头 */
    public static final String MESSAGE_BASE_HEADER_KEY = "MESSAGE_BASE_HEADER";
    /** 自定义内容格式 */
    public static final String CUSTEM_CONTENT_TYPE = "application/messagebase";

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Object content = null;
        MessageProperties properties = message.getMessageProperties();
        if (properties != null) {
            String contentType = properties.getContentType();
            if (contentType != null && contentType.equals(CUSTEM_CONTENT_TYPE)) {
                Map<String, Object> mappedHeaders = (Map<String, Object>) properties.getHeaders().get(MESSAGE_BASE_HEADER_KEY);
                MessageInfo info = new MessageInfo(message.getBody());
                info.getHeaders().putAll(mappedHeaders);
                return info;
            }
        }
        if (content == null) {
            content = message.getBody();
        }
        return content;
    }

    @Override
    protected Message createMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        byte[] bytes = null;
        if (object instanceof MessageInfo) {
            MessageInfo base = (MessageInfo) object;
            bytes = base.getBody();
            messageProperties.setHeader(MESSAGE_BASE_HEADER_KEY, base.getHeaders());
            messageProperties.setContentType(CUSTEM_CONTENT_TYPE);
            messageProperties.setMessageId(UUID.randomUUID().toString());
        }
        if (bytes != null) {
            messageProperties.setContentLength(bytes.length);
        }
        return new Message(bytes, messageProperties);
    }



}
