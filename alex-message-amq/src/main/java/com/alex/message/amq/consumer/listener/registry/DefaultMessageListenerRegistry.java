package com.alex.message.amq.consumer.listener.registry;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.amq.consumer.listener.MessageListenerDelegate;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.consumer.registry.AbstractMessageListenerRegistry;
import com.alex.message.utils.BeanUtils;
import com.alex.message.utils.SpringContextHolder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
@Component
public class DefaultMessageListenerRegistry extends AbstractMessageListenerRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageListenerRegistry.class);

    @Override
    protected void register(MessageHandler messageHandler) {
        String beanId = messageHandler.getClass().getSimpleName() + "_" + UUID.randomUUID() + "_ActiveMQ_MessageListener";
        registerBean(beanId, MessageListenerDelegate.class, null);

        MessageListenerDelegate messageListener = (MessageListenerDelegate) SpringContextHolder.getBean(beanId);
        messageListener.setMessageHandler(messageHandler);

        MessageListenerContainerConfig config = messageHandler.getMessageListenerContainerConfig();
        if (config.getPubSubDomain()) {
            // 发布订阅模式不支持多线程
            Assert.isTrue(config.getConcurrentConsumers().equals(1) && config.getMaxConcurrentConsumers().equals(1), "发布订阅模式不支持多线程消费");
        }

        String destName = config.getDestName();
        String consumerId = config.getConsumerId();
        if (config.getVirtualTopic() && StringUtils.isNotEmpty(consumerId)) {
            // 虚拟主题，需要对队列名称进行特殊处理
            destName = "Consumer." + consumerId + ".VirtualTopic." + destName;
        }

        Map<String, Object> properties = BeanUtils.toBean(config, Map.class);
        if (properties == null) {
            properties = new HashMap<>();
        }

        String errorHandlerName = (String) properties.get("errorHandlerName");
        String connectionFactoryName = (String) properties.get("connectionFactoryName");

        properties.put("destinationName", destName);
        properties.put("errorHandler", SpringContextHolder.getBean(StringUtils.defaultIfEmpty(errorHandlerName, "jmsErrorHandler")));
        properties.put("connectionFactory", SpringContextHolder.getBean(StringUtils.defaultIfEmpty(connectionFactoryName, "jmsFactory")));
        properties.put("messageListener", messageListener);
        if (!properties.containsKey("pubSubDomain")) {
            properties.put("pubSubDomain", false);
        }

        String messageListenerContainerName = beanId + "_MessageListenerContainer";
        registerBean(messageListenerContainerName, DefaultMessageListenerContainer.class, properties);
        DefaultMessageListenerContainer messageListenerContainer = (DefaultMessageListenerContainer) SpringContextHolder.getBean(messageListenerContainerName);
        messageListenerContainer.start();
        LOGGER.info("MessageListenerContainer:{} for destination {} is started，The listener is{}", messageListenerContainerName, destName, beanId);
    }

}
