package com.alex.message.rmq.consumer;

import com.sun.corba.se.pept.broker.Broker;

import java.util.List;

/**
 * RabbitMQ消息监听配置
 */
public class RabbitMessageListenerConfig {
    /** 队列名称 */
    private String simpleQueueName;
    /** 消息接受目标类型 */
    private Class<?> targetClass;
    /** 业务消息处理类 */
    private Object handleMessageObject;
    /** 消费者编号 */
    private String consumerId;
    /** 消费者线程数量 */
    private int concurrentConsumers;

    /** 多broker集群配置 */
    public List<Broker> brokers;

    /** 消息重试次数 */
    public int messageRetryCount;

    /**从方法名获取监听队列名称*/
    public boolean getDestNameByMethod;




    public int getMessageRetryCount() {
        return messageRetryCount;
    }

    public void setMessageRetryCount(int messageRetryCount) {
        this.messageRetryCount = messageRetryCount;
    }

    public List<Broker> getBrokers() {
        return brokers;
    }

    public void setBrokers(List<Broker> brokers) {
        this.brokers = brokers;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public Object getHandleMessageObject() {
        return handleMessageObject;
    }

    public void setHandleMessageObject(Object handleMessageObject) {
        this.handleMessageObject = handleMessageObject;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
    }


    public String getSimpleQueueName() {
        return simpleQueueName;
    }

    public void setSimpleQueueName(String simpleQueueName) {
        this.simpleQueueName = simpleQueueName;
    }



}
