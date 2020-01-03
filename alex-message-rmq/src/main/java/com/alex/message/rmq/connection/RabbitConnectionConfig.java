package com.alex.message.rmq.connection;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ连接配置类
 */
@Component
public class RabbitConnectionConfig {

    public static final String HOST = "rabbit.connection.host";

    public static final String PORT = "rabbit.connection.port";

    public static final String USERNAME = "rabbit.connection.username";

    public static final String PASSWORD = "rabbit.connection.password";

    public static final String VIRTUAL_HOST = "rabbit.connection.virtualHost";

    // 端口默认值
    private final static String DEFAULT_PORT = "5672";

    // Connection中缓存的Channel的数量，默认是 CPU核数 * 2
    public final static int DEFAULT_CHANNEL_CACHE_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    // broker主机连接地址
    @Value("${" + HOST + "}")
    public String host;

    // broker主机连接端口号
    @Value("${" + PORT + "}")
    public String port;

    // broker主机连接用户名
    @Value("${" + USERNAME + "}")
    public String username;

    // broker主机密码
    @Value("${" + PASSWORD + "}")
    public String password;

    // broker虚拟主机
    @Value("${" + VIRTUAL_HOST + "}")
    private String virtualHost;

    // Connection中缓存的Channel的数量
    private int channelCacheSize = DEFAULT_CHANNEL_CACHE_SIZE;

    public RabbitConnectionConfig() {
        super();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        if(StringUtils.isBlank(port)) {
            port = DEFAULT_PORT;
        }
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

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public int getChannelCacheSize() {
        return channelCacheSize;
    }

    public void setChannelCacheSize(int channelCacheSize) {
        this.channelCacheSize = channelCacheSize;
    }

}
