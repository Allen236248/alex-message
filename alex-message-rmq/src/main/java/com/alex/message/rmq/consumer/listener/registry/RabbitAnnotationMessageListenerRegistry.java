package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.rmq.consumer.listener.MessageListenerAttribute;
import com.alex.message.rmq.consumer.RabbitMessageListenerContainerConfig;
import com.alex.message.rmq.consumer.listener.AbstractMessageListener;
import com.alex.message.utils.SpringContextHolder;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * 消息容器注册类，用于收集消息监听配置，自动注册到Spring容器中
 */
@Component
public class RabbitAnnotationMessageListenerRegistry extends AbstractRabbitMessageListenerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitAnnotationMessageListenerRegistry.class);

    private volatile boolean started = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (started)
            return;

        String[] beanNames = SpringContextHolder.getApplicationContext().getBeanNamesForAnnotation(MessageListenerAttribute.class);
        if (null == beanNames || beanNames.length == 0)
            return;

        // 扫描所有增加RabbitListenerAttribute注解的类，自动注册所有实现RabbitListenerAttribute的监听器
        for (int i = 0; i < beanNames.length; i++) {
            Object bean = SpringContextHolder.getBean(beanNames[i]);
            // 旧业务逻辑兼容
            if (bean instanceof MessageHandler) {
                LOGGER.warn("{}不能同时使用实现MessageHandler和添加RabbitListenerAttribute两种注册方式", bean);
                continue;
            }
            MessageListenerAttribute attribute = bean.getClass().getAnnotation(MessageListenerAttribute.class);
            checkInstance(bean, attribute);
            // 监听自动注册
            register(bean, RabbitMessageListenerContainerConfig.build(attribute));
            started = true;
            LOGGER.info("message listener registry success");
        }
    }

    private void checkInstance(Object bean, MessageListenerAttribute attribute) {
        if (!(bean instanceof AbstractMessageListener)) {
            throw new MessageException("The message listener must extends AbstractMessageListener");
        }

        // 如果为持久化广播时，消费者编号不能为空
        if (attribute.isPersistentPublish() && StringUtils.isBlank(attribute.consumerId())) {
            LOGGER.error("persistentPublish must consumeId ,class:{}", bean);
            throw new MessageException("persistentPublish mode must  consumeId");
        }
    }

}
