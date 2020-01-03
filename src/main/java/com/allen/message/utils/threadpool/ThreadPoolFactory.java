package com.allen.message.utils.threadpool;


public class ThreadPoolFactory {

    private ThreadPoolFactory() {}

    /**
     * 创建线程池
     *
     * @param properties
     * @return
     */
    public static GeneralThreadPool create(ThreadPoolProperties properties) {
        return ThreadPoolManager.getInstance().create(properties);
    }

    /**
     * 关闭线程池
     *
     * @param executor
     */
    public static void shutdown(GeneralThreadPool executor) {
        ThreadPoolManager.getInstance().shutdown(executor);
    }

    /**
     * 最大可创建线程池数
     *
     * @return
     */
    public static int getMaxThreadPoolsAllowed() {
        return ThreadPoolManager.getInstance().getMaxThreadPoolsAllowed();
    }

    /**
     * 所有线程池配置信息(json格式)
     *
     * @return
     */
    public static String getInfo() {
        return ThreadPoolManager.getInstance().getPoolsInfo();
    }
}
