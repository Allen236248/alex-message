package com.alex.message;

import com.alibaba.fastjson.JSON;

import javax.jms.Session;
import java.io.Serializable;

/**
 * 消息监听容器配置
 */
public class MessageListenerContainerConfig implements Serializable {

    //并发消费者数量
    private int concurrentConsumers = 1;

    //最大并发消费者数量
    private Integer maxConcurrentConsumers = 1;

    /**
     * 可选，默认 false
     */
    private Boolean sessionTransacted = false;

    /**
     * 可选，默认 client ack
     */
    private Integer sessionAcknowledgeMode = Session.CLIENT_ACKNOWLEDGE;

    /**
     * 可选，连接工程名称，默认"jmsFactory"
     */
    private String connectionFactoryName;

    /**
     * 必须，目的地名称
     */
    private String destName;

    /**
     * 可选，消费者ID，消费虚拟主题时需要
     */
    private String consumerId;

    /**
     * 是否是虚拟主题,默认false
     */
    private Boolean virtualTopic = false;

    /**
     * 排它模式 ，默认false
     */
    private Boolean exclusive = false;
    
    /**
     * 发布订阅模式,默认fasle
     */
    private Boolean pubSubDomain = false;


    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }


    public Integer getConcurrentConsumers() {
        return concurrentConsumers;
    }


    public void setConcurrentConsumers(Integer concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }


    public Integer getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }


    public void setMaxConcurrentConsumers(Integer maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }


    public Boolean getSessionTransacted() {
        return sessionTransacted;
    }


    public void setSessionTransacted(Boolean sessionTransacted) {
        this.sessionTransacted = sessionTransacted;
    }


    public Integer getSessionAcknowledgeMode() {
        return sessionAcknowledgeMode;
    }


    public void setSessionAcknowledgeMode(Integer sessionAcknowledgeMode) {
        this.sessionAcknowledgeMode = sessionAcknowledgeMode;
    }


    public String getConnectionFactoryName() {
        return connectionFactoryName;
    }


    public void setConnectionFactoryName(String connectionFactoryName) {
        this.connectionFactoryName = connectionFactoryName;
    }


    public String getDestName() {
        return destName;
    }


    public void setDestName(String destName) {
        this.destName = destName;
    }


    public String getConsumerId() {
        return consumerId;
    }


    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }


    public Boolean getVirtualTopic() {
        return virtualTopic;
    }


    public void setVirtualTopic(Boolean virtualTopic) {
        this.virtualTopic = virtualTopic;
    }


    public Boolean getExclusive() {
        return exclusive;
    }


    public void setExclusive(Boolean exclusive) {
        this.exclusive = exclusive;
    }


    public Boolean getPubSubDomain() {
        return pubSubDomain;
    }


    public void setPubSubDomain(Boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }



}
