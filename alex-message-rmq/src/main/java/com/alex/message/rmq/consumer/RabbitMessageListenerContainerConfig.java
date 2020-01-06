package com.alex.message.rmq.consumer;

import com.alex.message.rmq.Broker;
import com.alex.message.rmq.consumer.listener.MessageListenerAttribute;
import com.alex.message.rmq.consumer.listener.MessageListenerAttribute.MultiBrokerListenerAttribute;

import java.util.ArrayList;
import java.util.List;

/**
 * 注解描述实体类
 */
public class RabbitMessageListenerContainerConfig {

    /** 必须目的地名称 */
    public String destName;

    /** 消费进程数 */
    public int concurrentConsumers;

    /** 是否为广播模式，消费端只有在启动才能接收消息,未在线期间广播消息会丢失. */
    public boolean isPublish;

    /** 持久化广播模式，消费者第一次链接程序 会自动注册，后期不管是否在线，均可以接受广播消息，与isPublish具有排它特性 */
    public boolean isPersistentPublish;

    /** 消费者编号 */
    public String consumerId;

    /** 多broker集群配置 */
    public List<Broker> brokers;

    /** 消息重试次数 */
    public int messageRetryCount = 2;

    /** 是否延时队列 */
    public boolean isDeadLetter;

    /**exchangeName*/
    public String otherExchangeName;

    public boolean isDeadLetter() {
        return isDeadLetter;
    }

    public void setDeadLetter(boolean isDeadLetter) {
        this.isDeadLetter = isDeadLetter;
    }

    public String getOtherExchangeName() {
        return otherExchangeName;
    }

    public void setOtherExchangeName(String otherExchangeName) {
        this.otherExchangeName = otherExchangeName;
    }

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

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public boolean isPublish() {
        return isPublish;
    }

    public void setPublish(boolean isPublish) {
        this.isPublish = isPublish;
    }

    public boolean isPersistentPublish() {
        return isPersistentPublish;
    }

    public void setPersistentPublish(boolean isPersistentPublish) {
        this.isPersistentPublish = isPersistentPublish;
    }

    public String getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(String consumerId) {
        this.consumerId = consumerId;
    }

    public static RabbitMessageListenerContainerConfig build(MessageListenerAttribute attribute) {
        RabbitMessageListenerContainerConfig config = new RabbitMessageListenerContainerConfig();
        config.setDestName(attribute.destName());
        config.setConcurrentConsumers(attribute.concurrentConsumers());
        config.setPublish(attribute.isPublish());
        config.setPersistentPublish(attribute.isPersistentPublish());
        config.setConsumerId(attribute.consumerId());
        // 设置消息重试次数
        config.setMessageRetryCount(attribute.messageRetryCount());
        // 设置其它需要绑定的exchangeName
        config.setOtherExchangeName(attribute.otherExchangeName());
        // 死信队列前缀
        if (attribute.isDeadLetter()) {
            config.setDestName(Broker.TTL + config.getDestName());
            config.setDeadLetter(attribute.isDeadLetter());
        }
        MultiBrokerListenerAttribute[] multiBrokerListenerAttributes = attribute.multiBrokerListenerAttribute();
        if (null != multiBrokerListenerAttributes && multiBrokerListenerAttributes.length > 0) {
            List<Broker> brokers = new ArrayList<>();
            for (MultiBrokerListenerAttribute multiBrokerListenerAttribute : multiBrokerListenerAttributes) {
                Broker broker = new Broker();
                broker.setBrokerName(multiBrokerListenerAttribute.brokerName());
                broker.setDestName(multiBrokerListenerAttribute.destName());
                brokers.add(broker);
            }
            config.setBrokers(brokers);
        }
        return config;
    }
}
