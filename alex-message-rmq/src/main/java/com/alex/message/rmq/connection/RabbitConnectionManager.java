package com.alex.message.rmq.connection;

import com.alex.message.exception.MessageException;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.converter.RabbitMessageConverter;
import com.alex.message.utils.PropertiesUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger LOGGER = LoggerFactory.getLogger(RabbitConnectionManager.class);

    @Autowired
    private RabbitConnectionConfig rabbitConnectionConfig;

    @Autowired
    private PropertiesUtils propertiesUtils;

    private Map<String, ? super AbstractExchange> exchanges = new HashMap<>();

    private Map<String, Queue> queues = new HashMap<String, Queue>();

    private Set<String> bindings = new HashSet<String>();

    private Map<String, CachingConnectionFactory> connectionFactoryCache = new HashMap<String, CachingConnectionFactory>();

    private Map<String, RabbitAdmin> rabbitAdminHolder = new HashMap<String, RabbitAdmin>();

    public static final String DEAD_QUEUE_PREFIX = "dlq.";

    /**
     * 初始化连接工厂
     */
    @PostConstruct
    public void initConnectionFactory() {
        String host = rabbitConnectionConfig.getHost();
        String port = rabbitConnectionConfig.getPort();
        String username = rabbitConnectionConfig.getUsername();
        String password = rabbitConnectionConfig.getPassword();
        if (StringUtils.isBlank(host) || StringUtils.isBlank(port) || StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            LOGGER.warn("初始化连接工厂配置host/port/username/password不能为空");
            return;
        }

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(Integer.valueOf(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        int channelCacheSize = rabbitConnectionConfig.getChannelCacheSize();
        connectionFactory.setChannelCacheSize(channelCacheSize);
        String virtualHost = rabbitConnectionConfig.getVirtualHost();
        if (StringUtils.isNotBlank(virtualHost)) {
            connectionFactory.setVirtualHost(virtualHost);
        }
        this.connectionFactoryCache.put(Broker.DEFAULT_BROKER_NAME, connectionFactory);
    }

    public CachingConnectionFactory getConnectionFactory(String brokerName) {
        if (this.connectionFactoryCache.containsKey(brokerName)) {
            return this.connectionFactoryCache.get(brokerName);
        }

        // 获取当前需要支持的所有broker
        String host = propertiesUtils.getPropertiesValue(brokerName + "." + RabbitConnectionConfig.HOST);
        String port = propertiesUtils.getPropertiesValue(brokerName + "." + RabbitConnectionConfig.PORT);
        String username = propertiesUtils.getPropertiesValue(brokerName + "." + RabbitConnectionConfig.USERNAME);
        String password = propertiesUtils.getPropertiesValue(brokerName + "." + RabbitConnectionConfig.PASSWORD);
        String virtualHost = propertiesUtils.getPropertiesValue(brokerName + "." + RabbitConnectionConfig.VIRTUAL_HOST);
        if (StringUtils.isBlank(host) || StringUtils.isBlank(username) || StringUtils.isBlank(password) || StringUtils.isBlank(port)) {
            throw new MessageException("未找到Broker " + brokerName + "的连接配置");
        }

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(Integer.valueOf(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        if (StringUtils.isNotBlank(virtualHost)) {
            connectionFactory.setVirtualHost(virtualHost);
        }
        this.connectionFactoryCache.put(brokerName, connectionFactory);
        return connectionFactory;
    }

    public RabbitAdmin getRabbitAdmin(String brokerName) {
        if (this.rabbitAdminHolder.containsKey(brokerName)) {
            return this.rabbitAdminHolder.get(brokerName);
        }
        RabbitAdmin rabbitAdmin = new RabbitAdmin(this.getConnectionFactory(brokerName));

        RabbitTemplate rabbitTemplate = rabbitAdmin.getRabbitTemplate();
        rabbitTemplate.setMessageConverter(new RabbitMessageConverter());
        rabbitTemplate.setRetryTemplate(RabbitRetryConfig.getRetryTemplate());

        rabbitAdminHolder.put(brokerName, rabbitAdmin);
        return rabbitAdmin;
    }

    /**
     * 点对点消息，持久化队列
     */
    public RabbitTemplate getRabbitTemplateForDirect(String queueName, String brokerName) {
        return getRabbitTemplateForDirect(queueName, false, brokerName);
    }

    /**
     * 点对点消息，持久化队列，延时队列
     */
    public RabbitTemplate getRabbitTemplateForDirect(String queueName, boolean isDelay, String brokerName) {
        String exchangeName = queueName + "_Exchange";
        String routingKey = queueName + "_Routing";
        declareBinding(exchangeName, "direct", queueName, routingKey, true, false, isDelay, brokerName);

        RabbitTemplate amqpTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        return amqpTemplate;
    }

    /**
     * 广播消息，持久化队列
     */
    public RabbitTemplate getRabbitTemplateForFanout(String topicName, String brokerName, boolean isPersistent) {
        declareBindingForFanout(topicName, null, isPersistent, brokerName);

        RabbitTemplate amqpTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        amqpTemplate.setExchange(topicName);
        return amqpTemplate;
    }


    /**
     * 提供广播功能，临时队列
     */
    public String getQueueNamePublish(String name, String brokerName, String consumerId, String exchangeName) {
        String queueName = "publish." + UUID.randomUUID() + "." + name.toLowerCase();
        declareBinding(name, "fanout", queueName, null, false, true, false, brokerName);
        if (StringUtils.isNotBlank(exchangeName)) {
            declareBinding(exchangeName, "fanout", queueName, null, false, true, false, brokerName);
        }
        return queueName;
    }

    /**
     * 提供广播功能，临时队列
     */
    public String getQueueNamePublish(String name, String brokerName, String consumerId) {
        String queueName = "publish." + UUID.randomUUID() + "." + name.toLowerCase();
        declareBindingForFanout(name, null, false, brokerName);
        return queueName;
    }

    /**
     * 提供广播功能，持久化队列
     */
    public String getQueueNamePersistentPublish(String topicName, String consumerId, String brokerName) {
        String queueName = null;
        if (null != consumerId) {
            queueName = "persistent.publish." + consumerId.toLowerCase() + "." + topicName.toLowerCase();
        }
        declareBindingForFanout(topicName, null, true, brokerName);
        return queueName;
    }

    /**
     * 提供广播功能，临时队列
     *
     * @param durable 是否为持久化队列
     */
    private void declareBindingForFanout(String exchangeName, String queueName, boolean durable, String brokerName) {
        boolean autoDelete = !durable;
        declareBinding(exchangeName, "fanout", queueName, null, durable, autoDelete, false, brokerName);
    }

    /**
     * 声明exchange和queue已经它们的绑定关系
     */
    private synchronized void declareBinding(String exchangeName, String exchangeType, String queueName, String routingKey, boolean durable,
                                             boolean autoDelete, boolean isDelay, String brokerName) {
        String bindRelation = brokerName + "-" + exchangeName + "-" + queueName;
        if (bindings.contains(bindRelation)) {
            return;
        }
        String prefix = isDelay ? Broker.TTL : DEAD_QUEUE_PREFIX;

        boolean needBinding = false;
        String exchangeKey = brokerName + "-" + exchangeName;
        AbstractExchange abstractExchange = (AbstractExchange) exchanges.get(exchangeKey);
        if (abstractExchange == null) {
            // 声明Exchange
            abstractExchange = declareExchange(exchangeName, exchangeType, durable, autoDelete, brokerName);
            exchanges.put(exchangeKey, abstractExchange);
            needBinding = true;
        }

        String queueKey = brokerName + "-" + queueName;
        String deadQueueKey = brokerName + "-" + prefix + queueName;
        Queue queue = queues.get(queueKey);
        Queue deadQueue = queues.get(deadQueueKey);
        if (queue == null && StringUtils.isNotBlank(queueName)) {
            queue = declareQueue(exchangeName, queueName, routingKey, durable, autoDelete, isDelay, brokerName, prefix);
            queues.put(queueKey, queue);

            //如果延时，实际生成的为延时队列
            deadQueue = declareDeadQueue(exchangeName, exchangeType, queueName, routingKey, durable, autoDelete, isDelay, brokerName, prefix);
            queues.put(deadQueueKey, deadQueue);

            needBinding = true;
        }

        if (needBinding) {
            Binding binding = bindingExchangeAndQueue(abstractExchange, exchangeType, routingKey, queue, brokerName);
            if (null != binding) {
                bindings.add(bindRelation);
            }
            bindingExchangeAndDeadQueue(exchangeName, exchangeType, routingKey, queue, deadQueue, durable, autoDelete, isDelay, brokerName, prefix);
        }
    }

    private AbstractExchange declareExchange(String exchangeName, String exchangeType, boolean durable, boolean autoDelete, String brokerName) {
        AbstractExchange abstractExchange = null;
        if ("direct".equals(exchangeType)) {
            abstractExchange = new DirectExchange(exchangeName, durable, autoDelete);
        } else if ("fanout".equals(exchangeType)) {
            abstractExchange = new FanoutExchange(exchangeName, durable, autoDelete);
        } else if ("topic".equals(exchangeType)) {
            abstractExchange = new TopicExchange(exchangeName, durable, autoDelete);
        }
        this.getRabbitAdmin(brokerName).declareExchange(abstractExchange);
        return abstractExchange;
    }

    /**
     * 创建队列：
     * 如果队列名称以dlq.开头，则为创建死信队列，直接创建
     * 否则创建队列，同时指定队列消息死信后，将消息发送到的交换器
     *
     * @param exchangeName
     * @param queueName
     * @param routingKey
     * @param durable
     * @param autoDelete
     * @param isDelay
     * @param brokerName
     * @return
     */
    private Queue declareQueue(String exchangeName, String queueName, String routingKey, boolean durable, boolean autoDelete, boolean isDelay, String brokerName, String prefix) {
        Queue queue = null;
        if (queueName.startsWith(DEAD_QUEUE_PREFIX)) {
            //死信队列
            queue = new Queue(queueName, durable, false, autoDelete);
        } else {
            //通过配置队列的x-dead-letter-exchange及x-dead-letter-routing-key键值，队列中消息死信后就会被重新发送到指定的Dead Letter Exchange中。
            Map<String, Object> arguments = new HashMap<String, Object>();
            //如果是延时队列，死信后进入名称为ttl.exchangeName的交换器中；如果是非延时队列，死信后进入名称为dlq.exchangeName的交换器中；
            arguments.put("x-dead-letter-exchange", prefix + exchangeName);
            arguments.put("x-dead-letter-routing-key", prefix + routingKey);
            queue = new Queue(queueName, durable, false, autoDelete, arguments);
        }
        this.getRabbitAdmin(brokerName).declareQueue(queue);
        return queue;
    }

    /**
     * 创建死信队列：
     * 如果队列名称以dlq.开头，则为创建死信队列，直接创建
     * 如果非延时，创建死信队列
     * 如果延时，创建延时队列（以ttl.为队列名前缀）的同时，指定队列消息死信后，将消息发送到的交换器
     *
     * @param exchangeName
     * @param exchangeType
     * @param queueName
     * @param routingKey
     * @param durable
     * @param autoDelete
     * @param isDelay
     * @param brokerName
     * @return
     */
    private Queue declareDeadQueue(String exchangeName, String exchangeType, String queueName, String routingKey, boolean durable, boolean autoDelete, boolean isDelay, String brokerName, String prefix) {
        if (queueName.startsWith(DEAD_QUEUE_PREFIX)) {
            //死信队列，不再创建死信队列
            return null;
        }
        Queue deadQueue = null;
        // 使用死信队列的要求：非广播消息或广播消息，但队列不自动删除
        if (("fanout".equals(exchangeType) && !autoDelete) || "direct".equals(exchangeType)) {
            if (isDelay) {
                Map<String, Object> arguments = new HashMap<String, Object>();
                arguments.put("x-dead-letter-exchange", DEAD_QUEUE_PREFIX + prefix + exchangeName);
                arguments.put("x-dead-letter-routing-key", DEAD_QUEUE_PREFIX + prefix + routingKey);
                deadQueue = new Queue(prefix + queueName, durable, false, autoDelete, arguments);
            } else {
                deadQueue = new Queue(prefix + queueName, durable, false, autoDelete);
            }
            this.getRabbitAdmin(brokerName).declareQueue(deadQueue);
        }
        return deadQueue;
    }

    private Binding bindingExchangeAndQueue(AbstractExchange abstractExchange, String exchangeType, String routingKey, Queue queue, String brokerName) {
        Binding binding = null;
        if ("direct".equals(exchangeType)) {
            DirectExchange directExchange = (DirectExchange) abstractExchange;
            binding = BindingBuilder.bind(queue).to(directExchange).with(routingKey);// 将queue绑定到exchange
            getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
        } else if ("fanout".equals(exchangeType)) {
            FanoutExchange fanoutExchange = (FanoutExchange) abstractExchange;
            if (queue != null) {
                binding = BindingBuilder.bind(queue).to(fanoutExchange);
                getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
            }
        } else if ("topic".equals(exchangeType)) {
            TopicExchange topicExchange = (TopicExchange) abstractExchange;
            binding = BindingBuilder.bind(queue).to(topicExchange).with(routingKey);// 将queue绑定到exchange
            getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
        }
        return binding;
    }

    private void bindingExchangeAndDeadQueue(String exchangeName, String exchangeType, String routingKey, Queue queue, Queue deadQueue, boolean durable, boolean autoDelete, boolean isDelay, String brokerName, String prefix) {
        if (null == deadQueue) {
            return;
        }

        // 声明死信队列绑定关系
        if (("fanout".equals(exchangeType) && !autoDelete) || "direct".equals(exchangeType)) {
            DirectExchange exchange = new DirectExchange(prefix + exchangeName, durable, autoDelete);
            this.getRabbitAdmin(brokerName).declareExchange(exchange);
            Binding binding = BindingBuilder.bind(deadQueue).to(exchange).with(prefix + routingKey);
            this.getRabbitAdmin(brokerName).declareBinding(binding);
        }

        if (isDelay) {
            // 如果是延时，创建了队列名为queue的队列，需要生成一个名为 ttl.exchange 的交换器。
            // 如果queue里的消息超时，消息将进入ttl.exchange的交换器，并被与此交换器绑定的队列（ttl.queue）消费。
            // 如果ttl.queue的消息消费失败了，将进入dlq.ttl.exchange的死信交换器，此交换器也需定义队列与之绑定
            DirectExchange deadTtlExchange = new DirectExchange(DEAD_QUEUE_PREFIX + prefix + exchangeName, durable, autoDelete);
            this.getRabbitAdmin(brokerName).declareExchange(deadTtlExchange);

            Queue deadTtlQueue = new Queue(DEAD_QUEUE_PREFIX + prefix + queue.getName(), durable, false, autoDelete);
            getRabbitAdmin(brokerName).declareQueue(deadTtlQueue); // 声明queue

            Binding deadttlBinding = BindingBuilder.bind(deadTtlQueue).to(deadTtlExchange).with(DEAD_QUEUE_PREFIX + prefix + routingKey);
            getRabbitAdmin(brokerName).declareBinding(deadttlBinding);
        }
    }

    // 其它应用优先加载
    @Override
    public int getOrder() {
        return 5000;
    }
}