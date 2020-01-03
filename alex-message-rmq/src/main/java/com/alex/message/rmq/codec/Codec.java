package com.alex.message.rmq.codec;

/**
 * 编码转换工厂
 */
public interface Codec {

    String DEFAULT_CHARSET = "UTF-8";

    String getContentType();

    <T> T decode(byte[] bytes, Class<T> targetClass);

    byte[] encode(Object obj);

}
