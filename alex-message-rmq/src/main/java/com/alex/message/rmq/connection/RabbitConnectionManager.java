package com.alex.message.rmq.connection;

import com.alex.message.exception.MessageException;
import com.alex.message.rmq.Broker;
import com.alex.message.rmq.converter.RabbitMessageConverter;
import com.alex.message.utils.PropertiesUtils;
import com.rabbitmq.http.client.domain.ExchangeType;
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

    private Map<String, AbstractExchange> exchanges = new HashMap<>();

    private Map<String, Queue> queues = new HashMap<String, Queue>();

    private Set<String> bindings = new HashSet<String>();

    private Map<String, CachingConnectionFactory> connectionFactoryCache = new HashMap<String, CachingConnectionFactory>();

    private Map<String, RabbitAdmin> rabbitAdminHolder = new HashMap<String, RabbitAdmin>();

    //Dead Letter Queue 死信队列
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
            LOGGER.error("初始化连接工厂配置host/port/username/password不能为空");
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

        //TODO 获取当前需要支持的所有Broker，此段逻辑意义是什么？
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
     * 点对点消息
     *
     * @param queueName
     * @param isDelay    是否延时
     * @param brokerName
     * @return
     */
    public RabbitTemplate getRabbitTemplateForDirect(String queueName, boolean isDelay, String brokerName) {
        declareBindingForDirect(queueName, true, isDelay, brokerName);

        RabbitTemplate rabbitTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        return rabbitTemplate;
    }

    /**
     * 获取Fanout类型（广播消息）的RabbitTemplate
     *
     * @param topicName
     * @param brokerName
     * @param durable    是否持久化
     * @return
     */
    public RabbitTemplate getRabbitTemplateForFanout(String topicName, String brokerName, boolean durable) {
        declareBindingForFanout(topicName, null, durable, brokerName);

        RabbitTemplate rabbitTemplate = this.getRabbitAdmin(brokerName).getRabbitTemplate();
        //TODO
        rabbitTemplate.setExchange(topicName);
        return rabbitTemplate;
    }

    /**
     * 点对点消息
     *
     * @param durable 是否为持久化队列
     */
    public void declareBindingForDirect(String queueName, boolean durable, boolean isDelay, String brokerName) {
        String exchangeName = queueName + "_Exchange";
        String routingKey = queueName + "_Routing";
        boolean autoDelete = !durable;
        declareBinding(exchangeName, ExchangeType.Direct, queueName, routingKey, durable, autoDelete, isDelay, brokerName);
    }

    /**
     * 提供广播功能，临时队列
     *
     * @param durable 是否为持久化队列
     */
    public void declareBindingForFanout(String exchangeName, String queueName, boolean durable, String brokerName) {
        boolean autoDelete = !durable;
        declareBinding(exchangeName, ExchangeType.Fanout, queueName, null, durable, autoDelete, false, brokerName);
    }

    /**
     * 声明exchange和queue以及它们的绑定关系
     */
    private synchronized void declareBinding(String exchangeName, ExchangeType exchangeType, String queueName, String routingKey, boolean durable,
                                             boolean autoDelete, boolean isDelay, String brokerName) {
        String bindRelation = brokerName + "-" + exchangeName + "-" + queueName;
        if (bindings.contains(bindRelation))
            return;

        boolean needBinding = false;
        String exchangeKey = brokerName + "-" + exchangeName;
        AbstractExchange abstractExchange = exchanges.get(exchangeKey);
        if (abstractExchange == null) {
            // 声明Exchange
            declareExchange(exchangeName, exchangeType, durable, autoDelete, brokerName, exchangeKey);
            abstractExchange = exchanges.get(exchangeKey);
            needBinding = true;
        }

        String prefix = isDelay ? Broker.TTL : DEAD_QUEUE_PREFIX;

        String queueKey = brokerName + "-" + queueName;
        String deadQueueKey = brokerName + "-" + prefix + queueName;
        Queue queue = queues.get(queueKey);
        Queue deadQueue = queues.get(deadQueueKey);
        if (queue == null && StringUtils.isNotBlank(queueName)) {
            declareQueue(exchangeName, exchangeType, queueName, routingKey, durable, autoDelete, isDelay, brokerName, queueKey, deadQueueKey);
            queue = queues.get(queueKey);
            deadQueue = queues.get(deadQueueKey);

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

    /**
     * 声明Exchange
     *
     * @param exchangeName 交换器名称
     * @param exchangeType 交换器类型
     * @param durable      交换器是否持久化，如果为true则服务器重启时不会丢失
     * @param autoDelete   交换器在不被使用时是否删除
     * @param brokerName
     */
    private void declareExchange(String exchangeName, ExchangeType exchangeType, boolean durable, boolean autoDelete, String brokerName, String exchangeKey) {
        AbstractExchange abstractExchange = null;
        if (ExchangeType.Direct.equals(exchangeType)) {
            abstractExchange = new DirectExchange(exchangeName, durable, autoDelete);
        } else if (ExchangeType.Fanout.equals(exchangeType)) {
            abstractExchange = new FanoutExchange(exchangeName, durable, autoDelete);
        } else if (ExchangeType.Topic.equals(exchangeType)) {
            abstractExchange = new TopicExchange(exchangeName, durable, autoDelete);
        }
        this.getRabbitAdmin(brokerName).declareExchange(abstractExchange);
        exchanges.put(exchangeKey, abstractExchange);
    }

    /**
     * 创建队列
     * 如果队列名称以dlq.开头，则为创建死信队列，直接创建
     * 否则创建队列，如果是非延时队列，则指定消息死信后绑定的交换器和路由键；
     * 如果是延时队列，则指定消息死信后绑定的交换器和路由键————创建延时队列，同时指定延时队列的消息再次死信后绑定的交换器和路由键；
     *
     * @param exchangeName 交换器名称
     * @param exchangeType
     * @param queueName    队列名称
     * @param routingKey   路由键
     * @param durable      队列是否持久化，为true时服务器r重启队列不会消失
     * @param autoDelete   队列在不被使用时是否自动删除（没有连接，并且没有未处理的消息)
     * @param isDelay      是否延时队列
     * @param brokerName
     * @param queueKey
     * @param deadQueueKey
     */
    private void declareQueue(String exchangeName, ExchangeType exchangeType, String queueName, String routingKey, boolean durable, boolean autoDelete, boolean isDelay, String brokerName, String queueKey, String deadQueueKey) {
        Queue queue = new Queue(queueName, durable, false, autoDelete, new HashMap<String, Object>());
        // 死信队列。exclusive：队列是否是独占的，如果为true只能被一个connection使用，其他连接建立时会抛出异常
        if (!queueName.startsWith(DEAD_QUEUE_PREFIX) && isDeadLetterQueueEnable(exchangeType, autoDelete)) {
            Queue deadQueue = null;
            if (isDelay) {
                //如果是延时队列，死信后进入名称为ttl.exchangeName的交换器中；如果是非延时队列，死信后进入名称为dlq.exchangeName的交换器中；
                queue.getArguments().put("x-dead-letter-exchange", Broker.TTL + exchangeName);
                queue.getArguments().put("x-dead-letter-routing-key", Broker.TTL + routingKey);

                deadQueue = new Queue(Broker.TTL + queueName, durable, false, autoDelete, new HashMap<String, Object>());
                deadQueue.getArguments().put("x-dead-letter-exchange", DEAD_QUEUE_PREFIX + Broker.TTL + exchangeName);
                deadQueue.getArguments().put("x-dead-letter-routing-key", DEAD_QUEUE_PREFIX + Broker.TTL + routingKey);
            } else {
                //通过配置队列的x-dead-letter-exchange及x-dead-letter-routing-key键值，队列中消息死信后就会被重新发送到指定的x-dead-letter-exchange中。
                queue.getArguments().put("x-dead-letter-exchange", DEAD_QUEUE_PREFIX + exchangeName);
                queue.getArguments().put("x-dead-letter-routing-key", DEAD_QUEUE_PREFIX + routingKey);

                deadQueue = new Queue(DEAD_QUEUE_PREFIX + queueName, durable, false, autoDelete);
            }
            this.getRabbitAdmin(brokerName).declareQueue(deadQueue);
            queues.put(deadQueueKey, deadQueue);
        }
        this.getRabbitAdmin(brokerName).declareQueue(queue);
        queues.put(queueKey, queue);
    }

    /**
     * 使用死信队列的要求：非广播消息或广播消息，但队列不自动删除
     *
     * @param exchangeType
     * @param autoDelete
     * @return
     */
    private boolean isDeadLetterQueueEnable(ExchangeType exchangeType, boolean autoDelete) {
        return ExchangeType.Direct.equals(exchangeType) || (ExchangeType.Fanout.equals(exchangeType) && !autoDelete);
    }

    private Binding bindingExchangeAndQueue(AbstractExchange abstractExchange, ExchangeType exchangeType, String routingKey, Queue queue, String brokerName) {
        Binding binding = null;
        if (ExchangeType.Direct.equals(exchangeType)) {
            DirectExchange directExchange = (DirectExchange) abstractExchange;
            binding = BindingBuilder.bind(queue).to(directExchange).with(routingKey);// 将queue绑定到exchange
            getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
        } else if (ExchangeType.Fanout.equals(exchangeType)) {
            FanoutExchange fanoutExchange = (FanoutExchange) abstractExchange;
            if (queue != null) {
                binding = BindingBuilder.bind(queue).to(fanoutExchange);
                getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
            }
        } else if (ExchangeType.Topic.equals(exchangeType)) {
            TopicExchange topicExchange = (TopicExchange) abstractExchange;
            binding = BindingBuilder.bind(queue).to(topicExchange).with(routingKey);// 将queue绑定到exchange
            getRabbitAdmin(brokerName).declareBinding(binding);// 声明绑定关系
        }
        return binding;
    }

    private void bindingExchangeAndDeadQueue(String exchangeName, ExchangeType exchangeType, String routingKey, Queue queue, Queue deadQueue, boolean durable, boolean autoDelete, boolean isDelay, String brokerName, String prefix) {
        if (null == deadQueue || !isDeadLetterQueueEnable(exchangeType, autoDelete))
            return;

        // 声明死信队列绑定关系
        DirectExchange exchange = new DirectExchange(prefix + exchangeName, durable, autoDelete);
        this.getRabbitAdmin(brokerName).declareExchange(exchange);
        Binding binding = BindingBuilder.bind(deadQueue).to(exchange).with(prefix + routingKey);
        this.getRabbitAdmin(brokerName).declareBinding(binding);

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

    private enum ExchangeType {
        Direct, Fanout, Topic
    }
}
