package com.allen.message.rmq.producer;

import com.allen.message.producer.MessageProducer;

/**
 * 通用消息服务接口
 * <p>
 * 针对发送方法中泛型对象的描述<br/>
 * 一、默认对象，默认的消息编码格式、消息头，默认采用FastJson对用户传入对象进行序列化操作 <br/>
 * 二、可自由定义消息头，需继承AbstractCustomMessageHeader抽象类，消息发送时，会在消息头中添加getCustomHarder方法中返回的数组.<br/>
 * 三、自封装MessageBase对象，消息内容自定义（byte类型），消息头(Map<String,Object>),其中消息头必须包含序列化类型.<br/>
 * </p>
 */
public interface RabbitMessageProducer extends MessageProducer {

    /**
     * 向队列发送消息 <br/>
     * <p>
     * 命名示例:queue.[domain].[module].queuename
     * </p>
     * 
     * @param queueName 队列名称（不可重复）
     * @param message 消息内容
     * @param isPersistent 消息是否持久化，持久化消息需要发送至持久化队列
     * @return void 返回类型
     * @author Frank 平台架构部
     * @date
     * @throws
     */
    void send(String queueName, Object message, boolean isPersistent);

    /**
     * 向队列发送消息 <br/>
     * <p>
     * 命名示例:queue.[domain].[module].queuename
     * </p>
     *
     * @param queueName 队列名称（不可重复）
     * @param message 消息内容
     * @return void 返回类型
     * @author Frank 平台架构部
     * @date
     * @throws
     */
    void send(String queueName, Object message, long delayTime);

    /**
     * 向队列发送消息 <br/>
     * 
     * broker接收到广播消息后会自动为订阅者创建一个持久化的队列用来存放广播消息. 订阅者不在线，系统会缓存消息，直到订阅者正常消费<br/ >
     * 每一个订阅者可最多一次成功消费该广播消息
     * 
     * <p>
     * 命名示例:persistent.publish.[domain].[module].queuename
     * </p>
     * 
     * @param topicName 订阅广播主题名称
     * @param message 消息内容
     * @param isPersistent 消息是否持久化，持久化消息需要发送至持久化队列
     * @return void 返回类型
     * @author Frank 平台架构部
     * @date
     * @throws
     */
    void publish(String topicName, Object message, boolean isPersistent);

}
