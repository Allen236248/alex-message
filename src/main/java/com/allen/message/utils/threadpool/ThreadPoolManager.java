package com.allen.message.utils.threadpool;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.Assert;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * 线程池管理者，单例
 *
 * 注: 用户可通过-DmaxThreadPoolsAllowed=xxx指定当前应用最大可创建线程池数量，默认为10
 *
 */
public class ThreadPoolManager implements ThreadPoolManagerMXBean {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    private static final int DEFAULT_MAX_THREAD_POOLS = 10;

    private int maxThreadPoolsAllowed = DEFAULT_MAX_THREAD_POOLS;

    private Map<Integer, ThreadPoolProperties> poolProperties = new HashMap<>();

    private int threadPoolSeq = 0;

    private static ThreadPoolManager instance = new ThreadPoolManager();

    static {
        setAppArguments();
        registerAsMBean();
    }

    private ThreadPoolManager() {}

    protected static ThreadPoolManager getInstance() {
        return instance;
    }
    protected synchronized GeneralThreadPool create(ThreadPoolProperties properties) {
        validateProperties(properties);
        if (poolProperties.size() < maxThreadPoolsAllowed) {
            // 便于JMX管理，此处深拷贝一份，因为不同的线程池可能使用同一个ThreadPoolProperties引用，
            // 这将导致poolProperties转json后出现引用式显示，从而不利于监控
            ThreadPoolProperties newProperties = new ThreadPoolProperties();
            BeanUtils.copyProperties(properties, newProperties);
            threadPoolSeq++;
            poolProperties.put(threadPoolSeq, newProperties);
            logger.info("CREATING THREAD POOL, id: {}", threadPoolSeq);
            return new GeneralThreadPool(threadPoolSeq, newProperties);
        } else {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.EXCEED_MAXIMUM_QUOTA, "maximum allowed: " + maxThreadPoolsAllowed);
        }
    }

    private void validateProperties(ThreadPoolProperties properties) {
        int corePoolSize = properties.getCorePoolSize();
        int maxPoolSize = properties.getMaxPoolSize();
        int queueCapacity = properties.getQueueCapacity();
        int priority = properties.getPriority();
        if (maxPoolSize <= 0) {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.INVALID_MAX_POOL_SIZE);
        }
        if (corePoolSize <= 0 || corePoolSize > maxPoolSize) {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.INVALID_CORE_POOL_SIZE);
        }
        if (queueCapacity < 0) {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.INVALID_QUEUE_CAPACITY);
        }
        if (!(priority >= 1 && priority <= 10)) {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.INVALID_PRIORITY);
        }
    }

    @Override
    public int getMaxThreadPoolsAllowed() {
        return maxThreadPoolsAllowed;
    }

    public void setMaxThreadPoolsAllowed(int maxThreadPoolsAllowed) {
        this.maxThreadPoolsAllowed = maxThreadPoolsAllowed;
    }

    @Override
    public int getThreadPoolCount() {
        return poolProperties.size();
    }

    @Override
    public String getPoolsInfo() {
        return JSON.toJSONString(poolProperties);
    }

    protected synchronized void shutdown(GeneralThreadPool customThreadPoolExecutor) {
        poolProperties.remove(customThreadPoolExecutor.getId());
        customThreadPoolExecutor.shutdown();
    }

    private static void setAppArguments() {
        Integer maxThreadPoolsArg = Integer.getInteger("maxThreadPoolsAllowed", DEFAULT_MAX_THREAD_POOLS);
        Assert.isTrue(maxThreadPoolsArg > 0, "maxThreadPoolsAllowed must be greater than 0");
        logger.info("set maxThreadPoolsAllowed to {}", maxThreadPoolsArg);
        instance.setMaxThreadPoolsAllowed(maxThreadPoolsArg);
    }

    private static void registerAsMBean() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName("yunnex.util.threadpool:type=ThreadPoolManager");
            mbs.registerMBean(instance, objectName);
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            logger.error("", e);
        }
    }
}
