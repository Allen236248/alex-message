package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.rmq.codec.Codec;
import com.alex.message.rmq.codec.CodecFactory;
import com.alex.message.rmq.consumer.RabbitMessageListenerContainerConfig;
import com.alex.message.rmq.consumer.listener.MessageListenerDelegate;
import com.alex.message.utils.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
@Component
public class DefaultRabbitMessageListenerRegistry extends AbstractRabbitMessageListenerRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(DefaultRabbitMessageListenerRegistry.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 注册所有的Codec
        registerCodec();

        super.onApplicationEvent(event);
    }

    @Override
    protected void register(MessageHandler messageHandler) {
        MessageListenerContainerConfig config = messageHandler.getMessageListenerContainerConfig();
        // 原始对象转换为新对象类型
        RabbitMessageListenerContainerConfig rabbitConfig = transformToRabbitMessageListenerContainerConfig(config);
        register(new MessageListenerDelegate(messageHandler), rabbitConfig);
    }

    private RabbitMessageListenerContainerConfig transformToRabbitMessageListenerContainerConfig(MessageListenerContainerConfig config) {
        RabbitMessageListenerContainerConfig rabbitConfig = new RabbitMessageListenerContainerConfig();
        rabbitConfig.setDestName(config.getDestName());
        rabbitConfig.setConcurrentConsumers(config.getMaxConcurrentConsumers());
        // 如果为虚拟主题 或 消费者订阅模式
        if (config.getVirtualTopic() || config.getPubSubDomain()) {
            rabbitConfig.setPublish(true);
            rabbitConfig.setConsumerId(config.getConsumerId());
        }
        rabbitConfig.setMessageRetryCount(2);
        return rabbitConfig;
    }

    /**
     * 查找项目中所有的协议转换类
     */
    private void registerCodec() {
        Map<String, Codec> beans = SpringContextHolder.getBeans(Codec.class);
        if (null == beans || beans.isEmpty())
            return;

        for (String bean : beans.keySet()) {
            Codec transCodec = beans.get(bean);
            CodecFactory.addCodec(transCodec);
        }
    }
}
