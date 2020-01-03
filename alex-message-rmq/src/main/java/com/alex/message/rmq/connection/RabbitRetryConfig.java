package com.alex.message.rmq.connection;

import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * 消息重发配置
 */
public class RabbitRetryConfig {

    // 第一次重发延时时间，单位：毫秒
    public static int REDELIVERY_DELAY = 1000;

    // 重发时间间隔递增倍数
    public static int BACKOFF_MULTIPLIER = 2;

    // 最大重发次数
    public static int MAX_REDELIVERY_TIMES = 2;

    /**
     * 最大重发延时。
     * 假设第一次重发延时为10ms，倍数为2，则第二次重发延时为20ms，第三次重发延时为40ms...
     * 当重发延时大于最大重发延时时，以后每次都以最大重发延时为准
     */
    public static int MAX_REDELIVERY_DELAY = 1000 * 60 * 10;

    public static RetryTemplate getRetryTemplate() {
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMultiplier(5);
        backOffPolicy.setMaxInterval(1000);

        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

}
