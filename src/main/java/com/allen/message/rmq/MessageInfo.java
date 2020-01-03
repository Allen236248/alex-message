package com.allen.message.rmq;

import java.util.HashMap;
import java.util.Map;

public class MessageInfo {

    // 序列化类型
    public final static String MESSAGE_CONTENT_TYPE = "message_content_type";

    // 内容大小
    public final static String MESSAGE_CONTENT_LENGTH = "message_content_length";

    // 消息队列名称
    public final static String MESSAGE_QUEUE_NAME = "queue_name";

    private Map<String, Object> headers = new HashMap<String, Object>();

    private byte[] body;

    private String brokerName;

    public MessageInfo() {

    }

    public MessageInfo(byte[] body) {
        setBody(body);
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
        this.headers.put(MESSAGE_CONTENT_LENGTH, body.length);
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public void setQueueName(String queueName) {
        this.headers.put(MESSAGE_QUEUE_NAME, queueName);
    }

    public String getQueueName() {
        return (String) this.headers.get(MESSAGE_QUEUE_NAME);
    }

    public void setContentType(String contentType) {
        this.headers.put(MESSAGE_CONTENT_TYPE, contentType);
    }

    public String getContentType() {
        return (String) this.headers.get(MESSAGE_CONTENT_TYPE);
    }

}
