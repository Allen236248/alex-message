package com.allen.message.utils.threadpool;

import java.io.Serializable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class ThreadPoolProperties implements Serializable {
    private static final long serialVersionUID = -4816128467893510345L;
    private int corePoolSize;
    private int maxPoolSize;
    private int queueCapacity;
    private String threadNamePrefix;
    private int priority = 5;
    private boolean isDaemon = false;
    private long keepAliveTime = 0L;
    private transient RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    /**
     * threadNamePrefix 设置线程池中线程名字统一前缀，通常用'-'分隔单词并以之结尾，e.g. yunnex-app-
     *
     * @param
     */
    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 设置线程中线程的优先级，取值范围为[1, 10]
     *
     * @param priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    /**
     * 设置线程中所有线程是否为daemon线程，默认为false
     *
     * @param isDaemon
     */
    public void setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * 当线程池中的线程数大于core线程数时，空闲线程在终止之前最长等待新任务的时间。默认为0，线程不终止
     * <p>
     * 单位：毫秒
     *
     * @param keepAliveTimeInMillis
     */
    public void setKeepAliveTime(long keepAliveTimeInMillis) {
        this.keepAliveTime = keepAliveTimeInMillis;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandler;
    }

    /**
     * 设置任务被拒绝执行策略，默认为ThreadPoolExecutor.AbortPolicy，将抛出RejectedExecutionException
     *
     * @param rejectedExecutionHandler
     */
    public void setRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        if (rejectedExecutionHandler == null) {
            return;
        }
        this.rejectedExecutionHandler = rejectedExecutionHandler;
    }
}
