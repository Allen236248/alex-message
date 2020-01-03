package com.alex.message.consumer.registry;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.utils.SpringContextHolder;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
public abstract class AbstractMessageListenerRegistry implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageListenerRegistry.class);

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 自动注册所有实现AutoRegister的监听器
        Map<String, MessageHandler> messageHandlers = SpringContextHolder.getBeans(MessageHandler.class);
        if (MapUtils.isEmpty(messageHandlers)) {
            LOGGER.warn("There is no MessageHandler implementations");
            return;
        }
        for (String beanName : messageHandlers.keySet()) {
            MessageHandler messageHandler = messageHandlers.get(beanName);
            register(messageHandler);
        }
    }

    protected abstract void register(MessageHandler messageHandler);

    /**
     * 向Spring容器注册一个bean定义
     */
    public void registerBean(String beanName, Class<?> bean, Map<String, Object> properties) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(bean);
        if (MapUtils.isNotEmpty(properties)) {
            Object target = null;
            try {
                target = bean.getConstructor().newInstance();
            } catch (Exception e) {
                LOGGER.error("实例化Bean失败，", e);
            }
            BeanWrapper propertyAccess = PropertyAccessorFactory.forBeanPropertyAccess(target);
            for (String key : properties.keySet()) {
                Object val = properties.get(key);
                if (val == null) {
                    continue;
                }
                PropertyDescriptor descriptor;
                try {
                    descriptor = propertyAccess.getPropertyDescriptor(key);
                } catch (InvalidPropertyException e) {
                    continue;// 没有该属性
                }
                Method writeMethod = descriptor.getWriteMethod();
                if (writeMethod == null) {
                    continue;
                }
                builder.addPropertyValue(key, val);
            }
        }
        AbstractApplicationContext context = (AbstractApplicationContext) SpringContextHolder.getApplicationContext();
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getBeanFactory();
        beanFactory.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

}
