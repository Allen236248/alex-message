package com.allen.message.utils.threadpool;

public interface ThreadPoolManagerMXBean {

    int getMaxThreadPoolsAllowed();

    int getThreadPoolCount();

    String getPoolsInfo();
}
