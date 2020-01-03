package com.allen.message.utils.threadpool;

public interface GeneralThreadPoolMXBean {

    int getMaximumPoolSize();

    int getActiveCount();

    long getCompletedTaskCount();

    int getLargestPoolSize();

    int getPoolSize();

    long getTaskCount();

    int getCorePoolSize();

    int getTaskRejected();

    int getQueueSize();

    int getQueueRemainingCapacity();
}
