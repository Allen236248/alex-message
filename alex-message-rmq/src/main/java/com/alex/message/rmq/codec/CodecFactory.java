package com.alex.message.rmq.codec;


import java.util.HashMap;
import java.util.Map;

/**
 * 序列化对象工厂
 */
public class CodecFactory {

    /** 序列化对象类型 */
    public static Map<String, Codec> CODEC_MAP = new HashMap<String, Codec>();

    /**
     * 添加序列化对象
     */
    public static void addCodec(Codec codec) {
        CODEC_MAP.put(codec.getContentType(), codec);
    }

    /**
     * 获取通用序列化对象
     */
    public static Codec getCodes(String contentType) {
        return CODEC_MAP.get(contentType);
    }

}
