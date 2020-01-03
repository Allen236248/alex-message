package com.alex.message.rmq.consumer.listener.registry;

import com.alex.message.MessageListenerContainerConfig;
import com.alex.message.consumer.handler.MessageHandler;
import com.alex.message.consumer.registry.AbstractMessageListenerRegistry;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.consumer.RabbitMessageListenerConfig;
import com.alex.message.rmq.consumer.RabbitMessageListenerContainerConfig;
import com.alex.message.rmq.consumer.handler.AbstractCodecMessageHandler;
import com.alex.message.rmq.consumer.handler.MessageHandleWrapper;
import com.alex.message.rmq.converter.RabbitMessageConverter;
import com.allen.message.utils.SpringContextHolder;
import org.aopalliance.aop.Advice;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.retry.interceptor.StatefulRetryOperationsInterceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息容器注册类 ，用于自动注册消息容器到spring容器中并启动
 */
public abstract class AbstractRabbitMessageListenerRegistry extends AbstractMessageListenerRegistry {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractRabbitMessageListenerRegistry.class);

    private static Map<String, SimpleMessageListenerContainer> simpleMessageListenerMap = new ConcurrentHashMap<>();

    private RabbitMessageListenerConfig rabbitMessageListenerConfig;

    @Override
    protected void register(MessageHandler messageHandler) {
        //empty implementation
    }

    public void register(Object messageHandler, RabbitMessageListenerContainerConfig config) {

        String destName = config.getDestName();
        // 初始化默认的broker监听器
        String defaultBrokerName = getBrokerName(destName, Broker.DEFAULT_BROKER_NAME);
        initRabbitConfig(config, messageHandler, defaultBrokerName, destName);
        registerListenerContainer(defaultBrokerName);

        // 额外的broker监听器
        List<Broker> brokers = config.getBrokers();
        if (CollectionUtils.isNotEmpty(brokers)) {
            for (Broker broker : brokers) {
                String brokerName = getBrokerName(config.getDestName(), broker.getBrokerName());
                String destNameOther = StringUtils.isNotBlank(broker.getDestName()) ? broker.getDestName() : config.getDestName();
                initRabbitConfig(config, messageHandler, brokerName, destNameOther);
                registerListenerContainer(brokerName);
            }
        }
    }

    public String getBrokerName(String queueName, String brokerName) {
        StringBuffer sb = new StringBuffer(brokerName);
        String grayEnv = System.getProperty(Broker.GRAY_ENV);
        String grayQueueStr = System.getProperty(Broker.GRAY_QUEUE_LIST);
        boolean isRegisterBetaBroker = false;
        // 是否有灰度队列
        if (StringUtils.isNotBlank(grayQueueStr)) {
            // 如果为*，表示所有队列都需监听灰度主机
            if (Broker.GRAY_QUEUE_MODLE_ALL.equals(grayQueueStr)) {
                isRegisterBetaBroker = true;
            } else {
                // 如果在灰度白名单中存在
                String[] grayQueueNameList = grayQueueStr.split(",");
                for (String grayQueueName : grayQueueNameList) {
                    if (queueName.equals(grayQueueName)) {
                        isRegisterBetaBroker = true;
                    }
                }
            }
            // 环境变量不为空，并且当前环境为灰度环境时，只监听灰度队列
            if (StringUtils.isNotBlank(grayEnv) && Broker.GRAY_BETA.equals(grayEnv) && isRegisterBetaBroker) {
                sb.append("_").append(grayEnv);
                return sb.toString();
            }
        }
        return brokerName;
    }

    /**
     * 获取监听器处理类
     */
    public MessageListenerAdapter getMessageListenerAdapter() {
        MessageListenerAdapter messageListener = new MessageListenerAdapter();
        messageListener.setDelegate(rabbitMessageListenerConfig.getHendleMessageObject());
        messageListener.setMessageConverter(new RabbitMessageConverter());
        return messageListener;
    }

    /**
     * 注册
     */
    public SimpleMessageListenerContainer registerListenerContainer(String brokerName) {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("connectionFactory", rabbitConnectionManager.getCachingConnectionFactory(brokerName));
        properties.put("acknowledgeMode", AcknowledgeMode.AUTO);
        properties.put("messageListener", getMessageListenerAdapter());
        properties.put("errorHandler", new MessageServiceErrorHandler(new DefaultFatalExceptionStrategy()));
        properties.put("queueNames", rabbitMessageListenerConfig.getSimpleQueueName());
        properties.put("concurrentConsumers", this.rabbitMessageListenerConfig.getConcurrentConsumers());
        properties.put("adviceChain", new Advice[] {retryInterceptor(this.rabbitMessageListenerConfig.getMessageRetryCount())});
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
        simpleMessageListenerMap.put(containerBeanName, container);
        // 如果状态为已启用或者为原始值，原始值是兼容老版本的做法
        if ("enable".equals(status) || "${_appstatus}".equals(status) || StringUtils.isEmpty(status)) {
            container.start();
            LOGGER.info("Messagelistener container:" + containerBeanName + " for destination '" + rabbitMessageListenerConfig.getSimpleQueueName() + "' started , Use the listener: " + this.rabbitMessageListenerConfig
                    .getHendleMessageObject().getClass().getName());
        } else {
            LOGGER.warn("Messagelistener container:" + containerBeanName + " for destination '" + rabbitMessageListenerConfig.getSimpleQueueName() + "' registered,but did not start,because the application is currently not enabled , Use the listener: " + this.rabbitMessageListenerConfig
                    .getHendleMessageObject().getClass().getName());
        }
        return container;
    }

    /**
     * 初始化rabbitMQConfig,并建立队列和exchange绑定关系
     *
     * @param @param config
     * @param @param hendleMessagebean
     * @param @param targetClass
     * @return void 返回类型
     * @author Frank 平台架构部
     * @date
     * @throws
     */
    public void initRabbitConfig(RabbitMessageListenerContainerConfig config, Object handler, String brokerName, String destName) {
        rabbitMessageListenerConfig = new RabbitMessageListenerConfig();
        String simpleQueueName;

        // 持久化订阅模式
        if (config.isPersistentPublish()) {
            simpleQueueName = rabbitConnectionManager.getQueueNamePersistentPublish(destName, config.getConsumerId(), brokerName);
        } else if (config.isPublish()) {
            // 广播类型，如果存在额外的otherExchage绑定关系
            if (StringUtils.isNotBlank(config.getOtheEexchangeName())) {
                simpleQueueName = rabbitConnectionManager.getQueueNamePublish(destName, brokerName, config.getConsumerId(),
                        config.getOtheEexchangeName());
            } else {
                simpleQueueName = rabbitConnectionManager.getQueueNamePublish(destName, brokerName, config.getConsumerId());
            }
        } else {
            // 是否延时队列
            if (config.isDeadLetter()) {
                // 替换为延时队列名称，用于向rabbitmq进行注册
                destName = destName.replace(BrokerConstant.TTL, "");
                simpleQueueName = rabbitConnectionManager.getQueueName(destName, brokerName, true);
                simpleQueueName = BrokerConstant.TTL + simpleQueueName;
            } else {
                simpleQueueName = rabbitConnectionManager.getQueueName(destName, brokerName);
            }

        }
        rabbitMessageListenerConfig.setSimpleQueueName(simpleQueueName);
        rabbitMessageListenerConfig.setHendleMessageObject(handler);
        rabbitMessageListenerConfig.setConcurrentConsumers(config.concurrentConsumers);
        rabbitMessageListenerConfig.setMessageRetryCount(config.getMessageRetryCount());
    }

    public void startAllMessageListener() {
        for (Map.Entry<String, SimpleMessageListenerContainer> entry : simpleMessageListenerMap.entrySet()) {
            SimpleMessageListenerContainer messageListenerContainer = entry.getValue();
            if (messageListenerContainer.isRunning()) {
                LOGGER.info("name:{} rabbit messagelistener is started", entry.getKey());
            } else {
                messageListenerContainer.start();
                LOGGER.info("start rabbit messagelistener success|name:{}", entry.getKey());
            }

        }
    }

    public void stopAllMessageListener() {
        for (Map.Entry<String, SimpleMessageListenerContainer> entry : simpleMessageListenerMap.entrySet()) {
            SimpleMessageListenerContainer messageListenerContainer = entry.getValue();
            if (!messageListenerContainer.isRunning()) {
                LOGGER.info("name:{} rabbit messagelistener is stoped", entry.getKey());
            } else {
                messageListenerContainer.stop();
                LOGGER.info("stop rabbit messagelistener success|name:{}", entry.getKey());
            }
        }
    }

    /**
     * 消息监听重试策略定义，当超过策略次数时，消息转发到消息队列
     *
     * @param @return
     * @return Advice 返回类型
     * @author Frank 平台架构部
     * @date
     * @throws
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
                .backOffOptions(MessageRetryConfig.initialRedeliveryDelay, MessageRetryConfig.backOffMultiplier,
                        MessageRetryConfig.maximumRedeliveryDelay).build();

    }

}
