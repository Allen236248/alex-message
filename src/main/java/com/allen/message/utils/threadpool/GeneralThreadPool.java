package com.allen.message.utils.threadpool;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * yunnex定制线程池，实现两个目标:
 *
 * 1. 传递调用线程上下文参数，如灰度标记；2. 规范线程使用方式，从而可以监控线程池状态
 *
 */
public class GeneralThreadPool implements GeneralThreadPoolMXBean {
    private static final Logger logger = LoggerFactory.getLogger(GeneralThreadPool.class);

    private ThreadPoolExecutor taskExecutor;

    private final AtomicInteger threadCount = new AtomicInteger(1);
    private final AtomicInteger taskRejected = new AtomicInteger(0);

    private ThreadPoolProperties properties;

    private String threadNamePrefix;

    private int id = 1;

    private static MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private final Thread shutdownHook = new Thread() {
        @Override
        public void run() {
            logger.info("shutting down threadpool {} along with system!", id);
            taskExecutor.shutdown();
        }
    };

    protected GeneralThreadPool(int threadPoolId, ThreadPoolProperties tpProperties) {
        this.properties = tpProperties;
        this.id = threadPoolId;
        this.threadNamePrefix = determineThreadNamePrefix();
        registerOrUnregisterAsMBean(true);
    }

    public void submit(Runnable task) {
        if (taskExecutor == null) {
            initTaskExecutor();
        }

        // 防止调用线程的context在异步线程复制之前变化，此处先复制一份到本地
        GeneralThreadContext copiedContext = duplicateContext(GeneralThreadContext.getContext().get());
        try {
            taskExecutor.execute(() -> {
                GeneralThreadContext.setContext(copiedContext);
                task.run();
            });
        } catch (RejectedExecutionException tre) {
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.TASK_REJECTED, "Executor: " + taskExecutor);
        }
    }

    public Future<?> submit(Callable<?> task) {
        if (taskExecutor == null) {
            initTaskExecutor();
        }

        // 防止调用线程的context在异步线程复制之前变化，此处先复制一份到本地
        GeneralThreadContext copiedContext = duplicateContext(GeneralThreadContext.getContext().get());
        try {
            return taskExecutor.submit((Callable<?>) () -> {
                GeneralThreadContext.setContext(copiedContext);
                return task.call();
            });
        } catch (RejectedExecutionException ree) {
            taskRejected.incrementAndGet();
            throw new ThreadPoolException(ThreadPoolException.ErrorCode.TASK_REJECTED, "Executor: " + taskExecutor);
        }
    }

    private synchronized void initTaskExecutor() {
        if (taskExecutor != null) { // 双检锁
            return;
        }
        BlockingQueue<Runnable> queue = createQueue(properties.getQueueCapacity());
        int corePoolSize = properties.getCorePoolSize();
        int maxPoolSize = properties.getMaxPoolSize();
        int priority = properties.getPriority();
        boolean isDaemon = properties.isDaemon();
        long keepAliveTime = properties.getKeepAliveTime();
        RejectedExecutionHandler rejectedExecutionHandler = properties.getRejectedExecutionHandler();

        taskExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, queue, runnable -> {
            Thread thread = new Thread(runnable);
            thread.setPriority(priority);
            thread.setName(nextThreadName());
            thread.setDaemon(isDaemon);
            return thread;
        }, rejectedExecutionHandler);

        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private String nextThreadName() {
        return threadNamePrefix + threadCount.getAndIncrement();
    }

    private String determineThreadNamePrefix() {
        StringBuilder strBuilder = new StringBuilder();
        String prefixSet = properties.getThreadNamePrefix();
        if (StringUtils.isEmpty(prefixSet)) {
            strBuilder.append("yunnex-threadpool-").append(id).append("-");
        } else if (!prefixSet.endsWith("-")) {
            strBuilder.append(prefixSet).append("-");
        }
        return strBuilder.toString();
    }

    private BlockingQueue<Runnable> createQueue(int queueCapacity) {
        if (queueCapacity > 0) {
            return new LinkedBlockingQueue<>(queueCapacity);
        } else {
            return new SynchronousQueue<>();
        }
    }

    /**
     * 复制ThreadLocal内容到本地变量
     *
     * @param callerContext
     * @return
     */
    private GeneralThreadContext duplicateContext(Map<String, Object> callerContext) {
        if (callerContext == null || callerContext.isEmpty()) {
            return null;
        }
        GeneralThreadContext localContext = new GeneralThreadContext();
        for (Map.Entry<String, Object> element : callerContext.entrySet()) {
            localContext.set(element.getKey(), element.getValue());
        }

        return localContext;
    }

    private void registerOrUnregisterAsMBean(boolean flag) {
        try {
            ObjectName objectName = new ObjectName("yunnex.util.threadpool:type=GeneralThreadPool-" + id);
            if (flag) {
                mBeanServer.registerMBean(this, objectName);
            } else {
                mBeanServer.unregisterMBean(objectName);
            }
        } catch (MalformedObjectNameException | InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException
                | InstanceNotFoundException e) {
            logger.error("", e);
        }
    }

    protected void shutdown() {
        logger.info("THREADPOOL {} IS SHUTTING DOWN..", id);
        registerOrUnregisterAsMBean(false);
        if (taskExecutor != null) {
            taskExecutor.shutdown();
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    protected int getId() {
        return id;
    }

    protected void setId(int id) {
        this.id = id;
    }

    @Override
    public int getQueueSize() {
        return taskExecutor.getQueue().size();
    }

    @Override
    public int getQueueRemainingCapacity() {
        return taskExecutor.getQueue().remainingCapacity();
    }

    @Override
    public int getTaskRejected() {
        return taskRejected.intValue();
    }

    @Override
    public int getMaximumPoolSize() {
        return taskExecutor.getMaximumPoolSize();
    }

    @Override
    public int getActiveCount() {
        return taskExecutor.getActiveCount();
    }

    @Override
    public long getCompletedTaskCount() {
        return taskExecutor.getCompletedTaskCount();
    }

    @Override
    public int getLargestPoolSize() {
        return taskExecutor.getLargestPoolSize();
    }

    @Override
    public int getPoolSize() {
        return taskExecutor.getPoolSize();
    }

    @Override
    public long getTaskCount() {
        return taskExecutor.getTaskCount();
    }

    @Override
    public int getCorePoolSize() {
        return taskExecutor.getCorePoolSize();
    }
}

