package com.alex.message.rmq;

/**
 * 注解描述实体类
 */
public class Broker {

    /**
     * 默认的broker
     */
    public static final String DEFAULT_BROKER_NAME = "DEFAULT_BROKER";

    /** 延时队列前缀 Time To Live */
    public static final String TTL = "ttl.";

    /** 目标名称 */
    private String destName;

    /** broker名称 */
    private String brokerName;

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }


}
