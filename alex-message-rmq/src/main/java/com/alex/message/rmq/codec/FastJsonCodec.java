package com.alex.message.rmq.codec;

import com.alex.message.exception.MessageCodecException;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * FastJson默认json格式化转换实现类
 */
@Component
public class FastJsonCodec implements Codec {

    private String charset = DEFAULT_CHARSET;

    public static final String CONTENT_TYPE = "application/fastjson";

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public <T> T decode(byte[] bytes, Class<T> targetClass) {
        T result = null;
        String content;
        try {
            content = new String(bytes, getCharset());
            result = JSON.parseObject(content, targetClass);
        } catch (Exception e) {
            throw new MessageCodecException("Failed to decode targetClass", e);
        }
        return result;
    }

    @Override
    public byte[] encode(Object obj) {
        byte[] bytes = null;
        try {
            String jsonString = JSON.toJSONString(obj);
            bytes = jsonString.getBytes(getCharset());
        } catch (IOException e) {
            throw new MessageCodecException("Failed to encode obj", e);
        }
        return bytes;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
