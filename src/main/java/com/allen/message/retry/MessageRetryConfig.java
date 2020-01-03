package com.allen.message.retry;

import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息重试机制
 */
public class MessageRetryConfig {

    /** 第一次重发延时时间，单位：毫秒 */
    public static int initialRedeliveryDelay = 1000;

    /** 重发时间间隔递增倍数 */
    public static int backOffMultiplier = 2;

    /** 最大重发次数 */
    public static int maximumRedeliveries = 2;

    /**
     * 最大重发延时假设第一次重发延时为10ms，倍数为2，则第二次重发延时为20ms 第三次重发延时为40ms，当重延时大于最大重发延时时，以后每次都以最大重发延时为准
     */
    public static int maximumRedeliveryDelay = 1000 * 60 * 10;

    public static RetryTemplate getSendRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMultiplier(5);
        backOffPolicy.setMaxInterval(1000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }



}
