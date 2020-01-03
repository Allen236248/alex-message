package com.allen.message.rmq.service;

import com.alex.message.exception.MessageException;
import com.allen.message.retry.MessageRetryConfig;
import com.allen.message.rmq.RabbitConfig;
import com.allen.message.utils.PropertiesUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.*;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class RabbitConnectionManager implements PriorityOrdered {

    @Autowired
    private RabbitConfig rabbitConfig;

    @Autowired
    private PropertiesUtils properties;

    private Map<String, ? super AbstractExchange> exchanges = new HashMap<String, AbstractExchange>();

    private Map<String, Queue> queues = new HashMap<String, Queue>();

    private Set<String> binded = new HashSet<String>();

    private Map<String, CachingConnectionFactory> connectionFactoryCache = new HashMap<String, CachingConnectionFactory>();

    public static final String DEAD_QUEUE_PREFIX = "dlq.";

    /**
     * 初始化连接工厂
     */
    @PostConstruct
    public void initConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        if (!rabbitConfig.getServerHost().contains("rabbitmq.conn.host")) {
            connectionFactory.setHost(rabbitConfig.getServerHost());
            connectionFactory.setChannelCacheSize(rabbitConfig.getEventMsgProcessNum());
            connectionFactory.setPort(Integer.valueOf(rabbitConfig.getPort()));
            connectionFactory.setUsername(rabbitConfig.getUsername());
            connectionFactory.setPassword(rabbitConfig.getPassword());
            if (!StringUtils.isEmpty(rabbitConfig.getVirtualHost())) {
                connectionFactory.setVirtualHost(rabbitConfig.getVirtualHost());
            }
            // 同时初始化灰度连接监听
            initConnectionFactoryGray(rabbitConfig, BrokerMode.DEFAULT_BROKER);
        } else {
            throw new MessageException("rabbitMq connectionFactory configuration value of broker cannot be obtained");
        }
        this.connectionFactoryCache.put(BrokerMode.DEFAULT_BROKER, connectionFactory);
    }

    /**
     * @param topicName
     * @param brokerName
     * @return
     * @date 2017年7月11日
     * @author zjq
     */
    public RabbitTemplate getTemplate(String topicName, String brokerName) {
        this.declareBinding(topicName, brokerName);
        RabbitTemplate amqpTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        amqpTemplate.setRetryTemplate(MessageRetryConfig.getSendRetryTemplate());
        amqpTemplate.setExchange(topicName);
        amqpTemplate.setMessageConverter(new RabbitMessageConverter());
        return amqpTemplate;
    }

    /**
     * @param topicName
     * @param brokerName
     * @return
     * @date 2017年7月11日
     * @author zjq
     */
    public RabbitTemplate getPersistentPublishTemplate(String topicName, String brokerName) {
        this.declareBindingPersistentPublish(topicName, brokerName);
        RabbitTemplate amqpTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        amqpTemplate.setRetryTemplate(MessageRetryConfig.getSendRetryTemplate());
        amqpTemplate.setExchange(topicName);
        amqpTemplate.setMessageConverter(new RabbitMessageConverter());
        return amqpTemplate;
    }


    public RabbitTemplate getTemplate(String brokerName) {
        RabbitTemplate amqpTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        amqpTemplate.setMessageConverter(new RabbitMessageConverter());
        amqpTemplate.setRetryTemplate(MessageRetryConfig.getSendRetryTemplate());
        return amqpTemplate;
    }

    public CachingConnectionFactory getCachingConnectionFactory(String brokerName) {
        // 根据brokerName返回broker工厂
        if (this.connectionFactoryCache.containsKey(brokerName)) {
            return this.connectionFactoryCache.get(brokerName);
        } else {
            // 获取当前需要支持的所有broker
            String host = properties.getPropertiesValue(brokerName + ".rabbitmq.conn.host");
            String username = properties.getPropertiesValue(brokerName + ".rabbitmq.conn.username");
            String password = properties.getPropertiesValue(brokerName + ".rabbitmq.conn.password");
            String port = properties.getPropertiesValue(brokerName + ".rabbitmq.conn.port");
            String virtualhost = properties.getPropertiesValue(brokerName + ".rabbitmq.conn.virtualhost");
            if (StringUtils.isBlank(host) || StringUtils.isBlank(username) || StringUtils.isBlank(password) || StringUtils.isBlank(port)) {
                throw new MessageException("The configuration value of broker cannot be obtained,brokerName:" + brokerName);
            }
            if (host.contains("rabbitmq.conn.host")) {
                throw new MessageException("The configuration value of broker cannot be obtained,brokerName:" + brokerName);
            }
            CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
            connectionFactory.setHost(host);
            connectionFactory.setPort(Integer.valueOf(port));
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
            if (StringUtils.isBlank(virtualhost)) {
                throw new MessageException("The configuration value of broker cannot be obtained,brokerName:" + brokerName);
            }
            connectionFactory.setVirtualHost(connectionFactory.getVirtualHost());
            this.connectionFactoryCache.put(brokerName, connectionFactory);
            // 根据环境变量初始化connectionFactoryGray
            RabbitConfig rabbitConfig = new RabbitConfig();
            rabbitConfig.setServerHost(host);
            rabbitConfig.setPort(port);
            rabbitConfig.setUsername(username);
            rabbitConfig.setPassword(password);
            rabbitConfig.setVirtualHost(virtualhost);
            rabbitConfig.setEventMsgProcessNum(RabbitConfig.DEFAULT_PROCESS_THREAD_NUM);
            initConnectionFactoryGray(rabbitConfig, brokerName);
            return connectionFactory;
        }
    }

    /**
     * @return 返回同步发送工具类
     * @date 2017年7月11日
     * @author zjq
     */
    public RabbitAdmin getRabbitAdmin(String brokerName) {
        return new RabbitAdmin(this.getCachingConnectionFactory(brokerName));
    }

    /**
     * 提供广播功能，临时队列
     *
     * @param name 主题名称
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author raokeyong
     */
    public String getQueueNamePublish(String name, String brokerName, String consumerId, String exchangeName) {
        String queueName = "publish." + UUID.randomUUID() + "." + name.toLowerCase();
        declareBinding(name, queueName, "fanout", null, false, true, null, brokerName, false);
        if (StringUtils.isNotBlank(exchangeName)) {
            declareBinding(exchangeName, queueName, "fanout", null, false, true, null, brokerName, false);
        }
        return queueName;
    }

    /**
     * 提供广播功能，临时队列
     *
     * @param name 主题名称
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author raokeyong
     */
    public String getQueueNamePublish(String name, String brokerName, String consumerId) {
        String queueName = "publish." + UUID.randomUUID() + "." + name.toLowerCase();
        declareBinding(name, queueName, "fanout", null, false, true, null, brokerName, false);
        return queueName;
    }


    /**
     * 提供广播功能，持久化队列队列
     *
     * @param topicName  主题名称
     * @param consumerId 业务主键
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author zjq
     */
    public String getQueueNamePersistentPublish(String topicName, String consumerId, String brokerName) {
        String queueName = null;
        if (null != consumerId) {
            queueName = "persistent.publish." + consumerId.toLowerCase() + "." + topicName.toLowerCase();
        }
        declareBinding(topicName, queueName, "fanout", null, true, false, null, brokerName, false);
        return queueName;
    }

    /**
     * 提供单发功能，持久化队列队列
     *
     * @param name
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author zjq
     */
    public String getQueueName(String name, String brokerName) {
        String queueName = name.toLowerCase();
        declareBinding(queueName, queueName, "direct", queueName, true, false, null, brokerName, false);
        return queueName;
    }

    /**
     * exchange和queue是否已经绑定
     */
    protected boolean beBinded(String exchangeName, String queueName, String brokerName) {
        return binded.contains(brokerName + "-" + exchangeName + "-" + queueName);
    }

    /**
     * 提供广播功能，临时队列
     *
     * @param exchangeName 主题名称
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author raokeyong
     */
    public synchronized void declareBinding(String exchangeName, String brokerName) {
        declareBinding(exchangeName, "fanout", null, false, true, null, brokerName);
    }

    /**
     * 提供广播功能，持久化队列
     *
     * @param exchangeName 主题名称
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author raokeyong
     */
    public synchronized void declareBindingPersistentPublish(String exchangeName, String brokerName) {
        declareBinding(exchangeName, "fanout", null, true, false, null, brokerName);
    }

    /**
     * @param exchangeName
     * @param exchangeType
     * @param routingKey
     * @param durable
     * @param autoDelete
     * @param arguments
     * @date 2017年7月7日
     * @author zjq
     */
    protected synchronized void declareBinding(String exchangeName, String exchangeType, String routingKey, boolean durable, boolean autoDelete,
                                               Map<String, Object> arguments, String brokerName) {
        declareBinding(exchangeName, null, exchangeType, routingKey, durable, autoDelete, arguments, brokerName, false);
    }


    /**
     * 声明exchange和queue已经它们的绑定关系
     */
    private synchronized void declareBinding(String exchangeName, String queueName, String exchangeType, String routingKey, boolean durable,
                                               boolean autoDelete, Map<String, Object> arguments, String brokerName, boolean isDelay) {
        String bindRelation = brokerName + "-" + exchangeName + "-" + queueName;
        if (binded.contains(bindRelation)) {
            return;
        }
        String queuePrefix;
        if (isDelay) {
            queuePrefix = BrokerConstant.TTL;
        } else {
            queuePrefix = DEAD_QUEUE_PREFIX;
        }

        boolean needBinding = false;
        String exchangeKey = brokerName + "-" + exchangeName;
        AbstractExchange abstractExchange = (AbstractExchange) exchanges.get(exchangeKey);
        // 声明exchange
        if (abstractExchange == null) {
            // String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
            if ("direct".equals(exchangeType)) {
                abstractExchange = new DirectExchange(exchangeName, durable, autoDelete, arguments);
            }
            if ("fanout".equals(exchangeType)) {
                abstractExchange = new FanoutExchange(exchangeName, durable, autoDelete, arguments);
            }
            if ("topic".equals(exchangeType)) {
                abstractExchange = new TopicExchange(exchangeName, durable, autoDelete, arguments);
            }
            exchanges.put(exchangeKey, abstractExchange);
            getRabbitAdmin(brokerName).declareExchange(abstractExchange);
            needBinding = true;
        }

        String queueKey = brokerName + "-" + queueName;
        String deadQueueKey = brokerName + "-" + queuePrefix + queueName;
        Queue queue = queues.get(queueKey);
        Queue deadQueue = queues.get(deadQueueKey);
        if (queue == null && queueName != null) {
            if (queueName.startsWith(DEAD_QUEUE_PREFIX)) {
                //死信队列
                queue = new Queue(queueName, durable, false, autoDelete);
                queues.put(queueKey, queue);
            } else {
                Map<String, Object> argumentsQueue = new HashMap<String, Object>();
                argumentsQueue.put("x-dead-letter-exchange", queuePrefix + queueName);
                argumentsQueue.put("x-dead-letter-routing-key", queuePrefix + queueName);
                if (isDelay) {
                    argumentsQueue.put("x-message-ttl", Integer.MAX_VALUE);
                }
                queue = new Queue(queueName, durable, false, autoDelete, argumentsQueue);
                queues.put(queueKey, queue);
                // 非广播类的才需要死信队列
                if (("fanout".equals(exchangeType) && !autoDelete) || "direct".equals(exchangeType)) {
                    if (isDelay) {
                        Map<String, Object> delayArgumentsQueue = new HashMap<String, Object>();
                        delayArgumentsQueue.put("x-dead-letter-exchange", DEAD_QUEUE_PREFIX + queuePrefix + queue.getName());
                        delayArgumentsQueue.put("x-dead-letter-routing-key", DEAD_QUEUE_PREFIX + queuePrefix + queue.getName());
                        deadQueue = new Queue(queuePrefix + queue.getName(), durable, false, autoDelete, delayArgumentsQueue);
                    } else {
                        deadQueue = new Queue(queuePrefix + queue.getName(), durable, false, autoDelete);
                    }
                    queues.put(deadQueueKey, deadQueue);
                    getRabbitAdmin(brokerName).declareQueue(deadQueue); // 声明queue
                }
            }

            getRabbitAdmin(brokerName).declareQueue(queue); // 声明queue
            needBinding = true;
        }

        bindingExchangeQueue(exchangeType, routingKey, bindRelation, needBinding, abstractExchange, queue, brokerName, deadQueue, durable,
                autoDelete, queuePrefix);
        // 如果为延时队列，同步创建延时队列的死信队列
        if (isDelay) {
            Queue deadTtlQueue = new Queue(DEAD_QUEUE_PREFIX + queuePrefix + queue.getName(), durable, false, autoDelete);
            getRabbitAdmin(brokerName).declareQueue(deadTtlQueue); // 声明queue
            DirectExchange deadTtlExchange = new DirectExchange(DEAD_QUEUE_PREFIX + queuePrefix + queue.getName(), durable, autoDelete);
            getRabbitAdmin(brokerName).declareExchange(deadTtlExchange);
            Binding deadttlBinding = BindingBuilder.bind(deadTtlQueue).to(deadTtlExchange).with(DEAD_QUEUE_PREFIX + queuePrefix + queue.getName());
            getRabbitAdmin(brokerName).declareBinding(deadttlBinding);
        }

    }

    private void bindingExchangeQueue(String exchangeType, String routingKey, String bindRelation, boolean needBinding,
                                      AbstractExchange abstractExchange, Queue queue, String brokerName, Queue deadQueue, boolean durable, boolean autoDelete,
                                      String queue_prefix) {
        Binding binding;
        if (needBinding) {
            if ("direct".equals(exchangeType)) {
                DirectExchange directExchange = (DirectExchange) abstractExchange;
                binding = BindingBuilder.bind(queue).to(directExchange).with(routingKey);// 将queue绑定到exchange
                getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
                binded.add(bindRelation);
            }
            if ("fanout".equals(exchangeType)) {
                FanoutExchange fanoutExchange = (FanoutExchange) abstractExchange;
                if (queue != null) {
                    binding = BindingBuilder.bind(queue).to(fanoutExchange);
                    getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
                    binded.add(bindRelation);
                }
            }
            if ("topic".equals(exchangeType)) {
                TopicExchange topicExchange = (TopicExchange) abstractExchange;
                binding = BindingBuilder.bind(queue).to(topicExchange).with(routingKey);// 将queue绑定到exchange
                getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
                binded.add(bindRelation);
            }
            // 声明死信队列绑定关系
            if (queue != null && !queue.getName().startsWith(DEAD_QUEUE_PREFIX)) {
                if (("fanout".equals(exchangeType) && !autoDelete) || "direct".equals(exchangeType)) {
                    DirectExchange deadExchange = new DirectExchange(queue_prefix + queue.getName(), durable, autoDelete);
                    getRabbitAdmin(brokerName).declareExchange(deadExchange);
                    Binding deadBinding = BindingBuilder.bind(deadQueue).to(deadExchange).with(queue_prefix + queue.getName());
                    getRabbitAdmin(brokerName).declareBinding(deadBinding);
                }
            }
        }
    }

    public void initConnectionFactoryGray(RabbitConfig rabbitConfig, String brokerName) {
        CachingConnectionFactory rabbitConnectionFactory = new CachingConnectionFactory();
        rabbitConnectionFactory.setHost(rabbitConfig.getServerHost());
        rabbitConnectionFactory.setChannelCacheSize(rabbitConfig.getEventMsgProcessNum());
        rabbitConnectionFactory.setPort(Integer.valueOf(rabbitConfig.getPort()));
        rabbitConnectionFactory.setUsername(rabbitConfig.getUsername());
        rabbitConnectionFactory.setPassword(rabbitConfig.getPassword());
        rabbitConnectionFactory.setVirtualHost(rabbitConfig.getVirtualHost() + "_" + BrokerConstant.GRAY_BETA);
        this.connectionFactoryCache.put(brokerName + "_" + BrokerConstant.GRAY_BETA, rabbitConnectionFactory);
    }

    /**
     * 延时队列
     *
     * @param name
     * @return 返回队列名称
     * @date 2017年7月7日
     * @author zjq
     */
    public String getQueueName(String name, String brokerName, boolean deadLetter) {
        String queueName = name.toLowerCase();
        declareBinding(queueName, queueName, "direct", queueName, true, false, null, brokerName, deadLetter);
        return queueName;
    }

    // 其它应用优先加载
    @Override
    public int getOrder() {
        return 5000;
    }
}
