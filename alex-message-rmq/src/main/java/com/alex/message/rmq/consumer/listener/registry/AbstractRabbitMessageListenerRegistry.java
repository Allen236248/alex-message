package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.consumer.registry.AbstractMessageListenerRegistry;
import com.alex.message.exception.MessageCodecException;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.connection.RabbitConnectionManager;
import com.alex.message.rmq.connection.RabbitRetryConfig;
import com.alex.message.rmq.consumer.RabbitMessageListenerContainerConfig;
import com.alex.message.rmq.converter.RabbitMessageConverter;
import com.alex.message.utils.SpringContextHolder;
import org.aopalliance.aop.Advice;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;
import org.springframework.util.ErrorHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
public abstract class AbstractRabbitMessageListenerRegistry extends AbstractMessageListenerRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractRabbitMessageListenerRegistry.class);

    @Autowired
    private RabbitConnectionManager rabbitConnectionManager;

    @Override
    protected void register(MessageHandler messageHandler) {
        //empty implementation
    }

    protected void register(Object messageListener, RabbitMessageListenerContainerConfig config) {
        // 初始化默认的broker监听器
        String brokerName = Broker.DEFAULT_BROKER_NAME;
        registerListenerContainer(config, messageListener, brokerName, config.getDestName());

        // 额外的broker监听器
        List<Broker> brokers = config.getBrokers();
        if (CollectionUtils.isNotEmpty(brokers)) {
            for (Broker broker : brokers) {
                brokerName = broker.getBrokerName();
                registerListenerContainer(config, messageListener, brokerName, broker.getDestName());
            }
        }
    }

    /**
     * 获取监听器处理类
     */
    public MessageListenerAdapter getMessageListenerAdapter(Object messageListener) {
        MessageListenerAdapter adapter = new MessageListenerAdapter();
        adapter.setDelegate(messageListener);
        adapter.setMessageConverter(new RabbitMessageConverter());
        return adapter;
    }

    /**
     * 注册
     */
    public SimpleMessageListenerContainer registerListenerContainer(RabbitMessageListenerContainerConfig config, Object messageListener, String brokerName, String destName) {
        String queueName = declareBinding(config, brokerName, destName);

        int concurrentConsumers = config.concurrentConsumers;
        int messageRetryCount = config.getMessageRetryCount();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("connectionFactory", rabbitConnectionManager.getConnectionFactory(brokerName));
        properties.put("acknowledgeMode", AcknowledgeMode.AUTO);
        properties.put("messageListener", getMessageListenerAdapter(messageListener));
        properties.put("errorHandler", new RabbitMessageListenerErrorHandler());
        properties.put("queueNames", queueName);
        //对每个listener在初始化的时候设置的并发消费者的个数。每个监听器，使用多个线程，各自开Channel进行消息消费。
        properties.put("concurrentConsumers", concurrentConsumers);
        properties.put("adviceChain", new Advice[]{retryInterceptor(messageRetryCount)});
        properties.put("recoveryInterval", 2000L);
        String containerBeanName = "";
        if (Broker.DEFAULT_BROKER_NAME.equals(brokerName)) {
            containerBeanName = messageListener.getClass().getSimpleName() + "_" + UUID.randomUUID() + "_RMQ_MessageListenerContainer";
        } else {
            containerBeanName = brokerName + "_" + messageListener.getClass().getSimpleName() + "_" + UUID.randomUUID() + "_RMQ_MessageListenerContainer";
        }
        registerBean(containerBeanName, SimpleMessageListenerContainer.class, properties);
        SimpleMessageListenerContainer container = (SimpleMessageListenerContainer) SpringContextHolder.getBean(containerBeanName);
        container.start();
        return container;
    }

    /**
     * 消费者启动时，声明交换器、队列、路由键及其绑定关系
     */
    private String declareBinding(RabbitMessageListenerContainerConfig config, String brokerName, String destName) {
        String queueName = "";
        // 持久化订阅模式
        if (config.isPublish()) {
            queueName = "publish." + config.getConsumerId()  + "." + destName;
            rabbitConnectionManager.declareBindingForFanout(destName, queueName, true, brokerName);
        } else {
            boolean isDelay = config.isDelay();
            queueName = destName;
            rabbitConnectionManager.declareBindingForDirect(queueName, true, isDelay, brokerName);
            // 关于延时的的理解是：如果是延时队列，队列名为q2，那么这时实际会生成一个 ttl.q2 的交换器。
            // 如果q2里的消息超时，消息将进入ttl.q2的交换器，并被与此交换器绑定的队列消费。ttl.q2的消息消费失败了，将进入dlq.ttl.q2的死信交换器
            if (isDelay) {
                // 如果是延时队列，实际是ttl.<queueName>队列获取消息的
                queueName = Broker.TTL + queueName;
            }
        }
        return queueName;
    }

    /**
     * 消息监听重试策略定义，当超过策略次数时，消息转发到消息队列
     */
    public StatefulRetryOperationsInterceptor retryInterceptor(int messageRetryCount) {
        return RetryInterceptorBuilder.stateful().recoverer(new MessageRecoverer() {

                    @Override
                    public void recover(Message message, Throwable cause) {
                        LOGGER.warn("The message will automatically retry processing failure message is routed to the dead letter queue.Message:{}", message);
                        throw new ListenerExecutionFailedException("Retry Policy Exhausted", new AmqpRejectAndDontRequeueException(cause), message);
                    }
                }).maxAttempts(messageRetryCount)
                .backOffOptions(RabbitRetryConfig.REDELIVERY_DELAY, RabbitRetryConfig.BACKOFF_MULTIPLIER, RabbitRetryConfig.MAX_REDELIVERY_DELAY).build();

    }

    public static class RabbitMessageListenerErrorHandler implements ErrorHandler {

        private final Logger LOGGER = LoggerFactory.getLogger(RabbitMessageListenerErrorHandler.class);

        /**
         * 异常处理策略
         */
        private final FatalExceptionStrategy exceptionStrategy;

        public RabbitMessageListenerErrorHandler() {
            this.exceptionStrategy = new DefaultFatalExceptionStrategy();
        }

        @Override
        public void handleError(Throwable t) {
            if (LOGGER.isWarnEnabled()) {
                // 当消息进行重试时，只获取业务处理方法的错误日志，并打印。隐藏系统自定义日志
                if (t.getCause() != null && t.getCause().getCause() != null && t.getCause().getCause().getCause() != null) {
                    LOGGER.warn("Execution of Rabbit message listener failed.", t.getCause().getCause().getCause());
                } else {
                    LOGGER.warn("Execution of Rabbit message listener failed.", t.getCause());
                }
            }
            if (!this.causeChainContainsARADRE(t) && this.exceptionStrategy.isFatal(t)) {
                throw new AmqpRejectAndDontRequeueException("Error Handler converted exception to fatal", t);
            }
        }

        private boolean causeChainContainsARADRE(Throwable t) {
            Throwable cause = t.getCause();
            while (cause != null) {
                if (cause instanceof AmqpRejectAndDontRequeueException) {
                    return true;
                }
                cause = cause.getCause();
            }
            return false;
        }
    }

    public static class DefaultFatalExceptionStrategy implements FatalExceptionStrategy {

        private final Logger LOGGER = LoggerFactory.getLogger(DefaultFatalExceptionStrategy.class);

        @Override
        public boolean isFatal(Throwable t) {
            if (t instanceof ListenerExecutionFailedException && isCauseFatal(t.getCause())) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Fatal message conversion error; message rejected; " + "it will be dropped or routed to a dead letter exchange, if so configured: " + ((ListenerExecutionFailedException) t)
                            .getFailedMessage());
                }
                return true;
            }
            return false;
        }

        private boolean isCauseFatal(Throwable cause) {
            return cause instanceof MessageConversionException
                    || cause instanceof org.springframework.messaging.converter.MessageConversionException
                    || cause instanceof MethodArgumentNotValidException
                    || cause instanceof MethodArgumentTypeMismatchException
                    || cause instanceof NoSuchMethodException
                    || cause instanceof ClassCastException || isUserCauseFatal(cause);
        }

        private boolean isUserCauseFatal(Throwable cause) {
            if (cause instanceof MessageCodecException) {
                return true;
            }
            return false;
        }

    }

}
