package com.alex.message.rmq.consumer.listener;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;


/**
 * 消息队列中间件监听注解<br />
 * 一、isPublish与isPersistentPublish具有排它特性，当两者都设置为true时，系统默认为持久化广播模式<br />
 * 二、destName具备多种含义，与isPublish、isPersistentPublish配合使用<br />
 * 三、实现本注解的同时必须继承yunnex.common.message.core.consumer.base.MessageListener接口，否则注册监听会失效<br />
 * 四、支持多broker监听,但broker配置信息需联系平台架构部增加.当不指定ManyBrokerMessageLister中的destName时，使用上层注解的destName
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface MessageListenerAttribute {
    /** 必须目的地名称 */
    String destName();

    /** 消费进程数 */
    int concurrentConsumers() default 5;

    /** 消息重试次数 */
    int messageRetryCount() default 2;

    /** 是否为广播模式，消费端只有在启动才能接收消息,未在线期间广播消息会丢失. */
    boolean isPublish() default false;

    /** 持久化广播模式，消费者第一次链接程序 会自动注册，后期不管是否在线，均可以接受广播消息，与isPublish具有排它特性 */
    boolean isPersistentPublish() default false;

    /** 消费者编号 当为广播、持久化广播模式时，且需输入此字段 */
    String consumerId() default "";

    /** 多broker消息监听 */
    MultiBrokerListenerAttribute[] multiBrokerListenerAttribute() default {};

    /** 绑定其它的exchanges */
    String otherExchangeName() default "";

    /** 延时监听 */
    boolean isDeadLetter() default false;

    /**
     * 可使用一个监听类实现多broker消息监听
     * <p>
     * 需确保多broker中的目标对象消息都是采用相同的序列化方式，否则系统无法解析
     * </p>
     * 
     * @author Frank 平台架构部
     * @date 2017年7月18日
     * @version V3.0.0
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface MultiBrokerListenerAttribute {
        /**
         * broker目标对象名称
         * 
         * @param @return
         * @return String 返回类型
         * @author Frank 平台架构部
         * @date
         * @throws
         */
        String destName() default "";

        /**
         * broker名称
         * <p>
         * 从broker需经过平台架构部配置后才生效
         * </p>
         * 
         * @param @return
         * @return String 返回类型
         * @author Frank 平台架构部
         * @date
         * @throws
         */
        String brokerName() default "";

    }

}
