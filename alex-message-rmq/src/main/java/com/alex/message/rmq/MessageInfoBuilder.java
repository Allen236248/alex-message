package com.alex.message.rmq;


import com.alex.message.exception.MessageException;
import com.alex.message.rmq.codec.Codec;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class MessageInfoBuilder {

    public static MessageInfo build(Codec codec, Object obj) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContentType(codec.getContentType());

        byte[] body = codec.encode(obj);
        messageInfo.setBody(body);
        return messageInfo;
    }

    public static MessageInfo build(Codec codec, Object obj, Map<String, Object> header) {
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setContentType(codec.getContentType());
        byte[] body = codec.encode(obj);
        messageInfo.setBody(body);
        messageInfo.getHeaders().putAll(header);
        return messageInfo;
    }

    public static MessageInfo build(MessageInfo msg) {
        if (msg.getBody() == null) {
            throw new MessageException("message body must not null!");
        }
        if (StringUtils.isBlank(msg.getContentType())) {
            throw new MessageException("message contentType  must not null!");
        }
        return msg;
    }

}
