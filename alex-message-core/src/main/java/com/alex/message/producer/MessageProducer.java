package com.alex.message.producer;

/**
 * 消息服务接口(已废弃，请使用新版消息服务)
 * 一、引用新版yunnex-message服务，使用方法http://wiki.corp.yunnex.com/pages/viewpage.action?pageId=14978357
 * 二、如需将旧版本队列迁移到新版本消息服务中,请联系平台架构部增加路由信息
 */
public interface MessageProducer {

    /**
     * 向队列发送消息
     */
    void send(String queueName, Object message);

    /**
     * 向指定主题广播消息
     */
    void publish(String topicName, Object message);

}
