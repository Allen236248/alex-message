package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.exception.MessageException;
import com.alex.message.rmq.consumer.listener.RabbitListenerAttribute;
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
public class AnnotationRabbitMessageListenerRegistry extends AbstractRabbitMessageListenerRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotationRabbitMessageListenerRegistry.class);

    private volatile boolean started = false;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (started)
            return;

        String[] beanNames = SpringContextHolder.getBeanNamesForAnnotation(RabbitListenerAttribute.class);
        if (null == beanNames || beanNames.length == 0)
            return;

        // 扫描所有增加RabbitListenerAttribute注解的类，自动注册所有实现RabbitListenerAttribute的监听器
        for (int i = 0; i < beanNames.length; i++) {
            Object bean = SpringContextHolder.getBean(beanNames[i]);
            // 旧业务逻辑兼容
            if (bean instanceof MessageHandler) {
                LOGGER.warn("The implementations of MessageHandler will be registered by RabbitDefaultMessageListenerRegistry");
                continue;
            }
            RabbitListenerAttribute attribute = bean.getClass().getAnnotation(RabbitListenerAttribute.class);
            if (!(bean instanceof AbstractMessageListener)) {
                LOGGER.warn("message listener must extends AbstractMessageListener");
                continue;
            }

            // 如果为持久化广播时，消费者编号不能为空
            if (attribute.isPublish() && StringUtils.isBlank(attribute.consumerId())) {
                LOGGER.error("consumeId can not be empty on persistentPublish is true, the bean is {}", bean);
                throw new MessageException("consumeId can not be empty");
            }
            // 监听自动注册
            register(bean, RabbitMessageListenerContainerConfig.build(attribute));
        }
        LOGGER.info("message listener registry success");
        started = true;
    }

}
