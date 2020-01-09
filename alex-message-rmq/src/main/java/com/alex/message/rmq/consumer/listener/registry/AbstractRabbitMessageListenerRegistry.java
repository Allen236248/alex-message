package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.consumer.registry.AbstractMessageListenerRegistry;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.connection.RabbitConnectionManager;
import com.alex.message.rmq.connection.RabbitRetryConfig;
import com.alex.message.rmq.consumer.RabbitMessageListenerConfig;
import com.alex.message.rmq.consumer.RabbitMessageListenerContainerConfig;
import com.alex.message.rmq.converter.RabbitMessageConverter;
import com.alex.message.utils.SpringContextHolder;
import org.aopalliance.aop.Advice;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
public abstract class AbstractRabbitMessageListenerRegistry extends AbstractMessageListenerRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractRabbitMessageListenerRegistry.class);

    private RabbitMessageListenerConfig rabbitMessageListenerConfig;

    @Autowired
    private RabbitConnectionManager rabbitConnectionManager;

    @Override
    protected void register(MessageHandler messageHandler) {
        //empty implementation
    }

    protected void register(Object messageListener, RabbitMessageListenerContainerConfig config) {
        String destName = config.getDestName();
        // 初始化默认的broker监听器
        String brokerName = Broker.DEFAULT_BROKER_NAME;
        initRabbitConfig(config, messageListener, brokerName, destName);
        registerListenerContainer(brokerName);

        // 额外的broker监听器
        List<Broker> brokers = config.getBrokers();
        if (CollectionUtils.isNotEmpty(brokers)) {
            for (Broker broker : brokers) {
                brokerName = broker.getBrokerName();
                String destNameOther = StringUtils.isNotBlank(broker.getDestName()) ? broker.getDestName() : config.getDestName();
                initRabbitConfig(config, messageListener, brokerName, destNameOther);
                registerListenerContainer(brokerName);
            }
        }
    }

    /**
     * 初始化rabbitMQConfig,并建立队列和exchange绑定关系
     */
    public void initRabbitConfig(RabbitMessageListenerContainerConfig config, Object messageListener, String brokerName, String destName) {
        rabbitMessageListenerConfig = new RabbitMessageListenerConfig();
        String simpleQueueName;

        // 持久化订阅模式
        if (config.isPersistentPublish()) {
            simpleQueueName = rabbitConnectionManager.getQueueNamePersistentPublish(destName, config.getConsumerId(), brokerName);
        } else if (config.isPublish()) {
            // 广播类型，如果存在额外的otherExchange绑定关系
            String otherExchangeName = config.getOtherExchangeName();
            if (StringUtils.isNotBlank(otherExchangeName)) {
                simpleQueueName = rabbitConnectionManager.getQueueNamePublish(destName, brokerName, config.getConsumerId(),
                        otherExchangeName);
            } else {
                simpleQueueName = rabbitConnectionManager.getQueueNamePublish(destName, brokerName, config.getConsumerId());
            }
        } else {
            // 是否延时队列
            if (config.isDeadLetter()) {
                // 替换为延时队列名称，用于向rabbitmq进行注册
                destName = destName.replace(Broker.TTL, "");
                simpleQueueName = rabbitConnectionManager.getQueueName(destName, brokerName, true);
                simpleQueueName = Broker.TTL + simpleQueueName;
            } else {
                simpleQueueName = rabbitConnectionManager.getQueueName(destName, brokerName);
            }
        }
        rabbitMessageListenerConfig.setSimpleQueueName(simpleQueueName);
        rabbitMessageListenerConfig.setHandleMessageObject(messageListener);
        rabbitMessageListenerConfig.setConcurrentConsumers(config.concurrentConsumers);
        rabbitMessageListenerConfig.setMessageRetryCount(config.getMessageRetryCount());
    }

    /**
     * 获取监听器处理类
     */
    public MessageListenerAdapter getMessageListenerAdapter() {
        MessageListenerAdapter messageListener = new MessageListenerAdapter();
        messageListener.setDelegate(rabbitMessageListenerConfig.getHandleMessageObject());
        messageListener.setMessageConverter(new RabbitMessageConverter());
        return messageListener;
    }

    /**
     * 注册
     */
    public SimpleMessageListenerContainer registerListenerContainer(String brokerName) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("connectionFactory", rabbitConnectionManager.getConnectionFactory(brokerName));
        properties.put("acknowledgeMode", AcknowledgeMode.AUTO);
        properties.put("messageListener", getMessageListenerAdapter());
        properties.put("errorHandler", new RabbitListenerErrorHandler(new DefaultFatalExceptionStrategy()));
        properties.put("queueNames", rabbitMessageListenerConfig.getSimpleQueueName());
        properties.put("concurrentConsumers", this.rabbitMessageListenerConfig.getConcurrentConsumers());
        properties.put("adviceChain", new Advice[]{retryInterceptor(this.rabbitMessageListenerConfig.getMessageRetryCount())});
        properties.put("recoveryInterval", 2000L);
        String containerBeanName = "";
        if (Broker.DEFAULT_BROKER_NAME.equals(brokerName)) {
            //messageHandler.getClass().getSimpleName() + "_" + UUID.randomUUID() + "_ActiveMQ_MessageListener";
            containerBeanName = String.format("%04d", threadSize.incrementAndGet()) + "_" + rabbitMessageListenerConfig.getHandleMessageObject().getClass()
                    .getSimpleName() + "_simple";
            registerBean(containerBeanName, SimpleMessageListenerContainer.class, properties);
        } else {
            containerBeanName = brokerName + "_" + String.format("%04d", threadSize.incrementAndGet()) + "_" + rabbitMessageListenerConfig.getHandleMessageObject()
                    .getClass().getSimpleName() + "_simple";
            registerBean(containerBeanName, SimpleMessageListenerContainer.class, properties);
        }
        SimpleMessageListenerContainer container = SpringContextHolder.getBean(containerBeanName);
        container.start();
        return container;
    }

    /**
     * 消息监听重试策略定义，当超过策略次数时，消息转发到消息队列
     *
     * @param @return
     * @return Advice 返回类型
     * @throws
     * @author Frank 平台架构部
     * @date
     */
    public StatefulRetryOperationsInterceptor retryInterceptor(int messageRetryCount) {
        return RetryInterceptorBuilder
                .stateful()
                .recoverer(new MessageRecoverer() {

                    @Override
                    public void recover(Message message, Throwable cause) {
                        LOGGER.warn("The message will automatically retry processing failure message is routed to the dead letter queue.Message:{}", message);
                        throw new ListenerExecutionFailedException("Retry Policy Exhausted", new AmqpRejectAndDontRequeueException(cause), message);
                    }
                })
                .maxAttempts(messageRetryCount)
                .backOffOptions(RabbitRetryConfig.REDELIVERY_DELAY, RabbitRetryConfig.BACKOFF_MULTIPLIER,
                        RabbitRetryConfig.MAX_REDELIVERY_DELAY).build();

    }

    public static class RabbitListenerErrorHandler implements ErrorHandler {

        protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

        /**
         * 异常处理策略
         */
        private final FatalExceptionStrategy exceptionStrategy;

        public RabbitListenerErrorHandler() {
            this.exceptionStrategy = new DefaultExceptionStrategy();
        }


        public RabbitListenerErrorHandler(FatalExceptionStrategy exceptionStrategy) {
            this.exceptionStrategy = exceptionStrategy;
        }

        @Override
        public void handleError(Throwable t) {
            if (this.logger.isWarnEnabled()) {
                // 当消息进行重试时，只获取业务处理方法的错误日志，并打印。隐藏系统自定义日志
                if (t.getCause() != null && t.getCause().getCause() != null && t.getCause().getCause().getCause() != null) {
                    this.logger.warn("Execution of Rabbit message listener failed.", t.getCause().getCause().getCause());
                } else {
                    this.logger.warn("Execution of Rabbit message listener failed.", t.getCause());
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

    public static class DefaultExceptionStrategy implements FatalExceptionStrategy {

        protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

        @Override
        public boolean isFatal(Throwable t) {
            if (t instanceof ListenerExecutionFailedException && isCauseFatal(t.getCause())) {
                if (this.logger.isWarnEnabled()) {
                    this.logger.warn("Fatal message conversion error; message rejected; " + "it will be dropped or routed to a dead letter exchange, if so configured: " + ((ListenerExecutionFailedException) t)
                            .getFailedMessage());
                }
                return true;
            }
            return false;
        }

        private boolean isCauseFatal(Throwable cause) {
            return cause instanceof MessageConversionException || cause instanceof org.springframework.messaging.converter.MessageConversionException || cause instanceof MethodArgumentNotValidException || cause instanceof MethodArgumentTypeMismatchException || cause instanceof NoSuchMethodException || cause instanceof ClassCastException || isUserCauseFatal(cause);
        }

    }

}
