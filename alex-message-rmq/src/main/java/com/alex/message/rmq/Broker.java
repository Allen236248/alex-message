package com.alex.message.rmq;

/**
 * 注解描述实体类
 */
public class Broker {

    /**
     * 默认的broker
     */
    public static final String DEFAULT_BROKER_NAME = "DEFAULT_BROKER";

    /** 系统环境变量 */
    public static final String GRAY_ENV = "_GRAY_ENV";

    /** 灰度环境变量 */
    public static final String GRAY_BETA = "BETA";

    /** 正常生产环境变量 */
    public static final String GRAY_STABLE = "STABLE";

    /** 灰度队列白名单 */
    public static final String GRAY_QUEUE_LIST = "_GRAY_QUEUE_LIST";

    /** 全部队列 */
    public static final String GRAY_QUEUE_MODLE_ALL="*";

    /** 延时队列前缀 */
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
