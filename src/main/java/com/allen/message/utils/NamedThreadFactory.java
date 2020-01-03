package com.allen.message.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private static final AtomicInteger POOL_SEQ = new AtomicInteger(1);

    private final AtomicInteger threadCount = new AtomicInteger(1);

    private final String prefix;

    private final boolean deamon;

    private final ThreadGroup group;

    public NamedThreadFactory() {
        this("pool-" + POOL_SEQ.getAndIncrement(), false);
    }

    public NamedThreadFactory(String prefix) {
        this(prefix, false);
    }

    public NamedThreadFactory(String prefix, boolean deamon) {
        this.prefix = prefix + "-thread-";
        this.deamon = deamon;
        SecurityManager s = System.getSecurityManager();
        group = (s == null) ? Thread.currentThread().getThreadGroup() : s.getThreadGroup();
    }

    @Override
    public Thread newThread(Runnable runnable) {
        String name = prefix + threadCount.getAndIncrement();
        Thread ret = new Thread(group, runnable, name, 0);
        ret.setDaemon(deamon);
        return ret;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }
}