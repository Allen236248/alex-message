package com.allen.message.rmq;


import com.alex.message.exception.MessageException;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class MessageInfoBuilder {

    public static MessageInfo build(Codec codec, Object obj, String queueName) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setQueueName(queueName);
        messageInfo.setContentType(codec.getContentType());

        byte[] body = codec.encode(obj);
        messageInfo.setBody(body);
        return messageInfo;
    }

    public static MessageInfo build(Codec codec, Object obj, String queueName, Map<String, Object> header) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setQueueName(queueName);
        messageInfo.setContentType(codec.getContentType());
        byte[] body = codec.encode(obj);
        messageInfo.setBody(body);
        messageInfo.getHeaders().putAll(header);
        return messageInfo;
    }

    public static MessageInfo build(MessageInfo msg, String queueName) {
        if (msg.getBody() == null) {
            throw new MessageException("message body must not null!");
        }
        if (StringUtils.isBlank(msg.getContentType())) {
            throw new MessageException("message contentType  must not null!");
        }
        msg.setQueueName(queueName);
        return msg;
    }

}
