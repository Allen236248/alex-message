package com.allen.message.rmq;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ消息服务配置类
 */
@Component
public class RabbitConfig {

    /** 端口默认值 */
    private final static int DEFAULT_PORT = 5672;

    /** 事件消息处理线程数 事件消息处理线程数，默认是 CPU核数 * 2 */
    public final static int DEFAULT_PROCESS_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;

    /** 每次消费消息的预取值 */
    private static final int PREFETCH_SIZE = 1;

    /** broker连接地址 */
    @Value("${rabbitmq.conn.host}")
    public String serverHost;

    /** 连接端口号 */
    @Value("${rabbitmq.conn.port}")
    public String port;

    @Value("${rabbitmq.conn.virtualhost}")
    private String virtualHost;

    /** 连接用户名 */
    @Value("${rabbitmq.conn.username}")
    public String username;

    /** 连接密码 */
    @Value("${rabbitmq.conn.password}")
    public String password;

    /** 消息转换类型 */
    @Value("${rabbitmq.message.converter.type}")
    private String messageConverterType;

    /** 事件消息处理线程数，默认是 CPU核数 * 2 */
    private int eventMsgProcessNum = DEFAULT_PROCESS_THREAD_NUM;

    /** 每次消费消息的预取值 */
    private int prefetchSize = PREFETCH_SIZE;

    /** 建立连接的超时时间 */
    public int connectionTimeout = 0;

    public RabbitConfig() {
        super();
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public int getEventMsgProcessNum() {
        return eventMsgProcessNum;
    }

    public int getPrefetchSize() {
        return prefetchSize;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public void setEventMsgProcessNum(int eventMsgProcessNum) {
        this.eventMsgProcessNum = eventMsgProcessNum;
    }

    public void setPrefetchSize(int prefetchSize) {
        this.prefetchSize = prefetchSize;
    }

}
