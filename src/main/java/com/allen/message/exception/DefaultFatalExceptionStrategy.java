package com.allen.message.exception;

import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;

/**
 * 扩展spring-amqp的致命异常
 *
 * @author Frank 平台架构部
 * @date 2017年7月19日
 * @version V3.0.0
 */
public class DefaultFatalExceptionStrategy extends MessageServiceErrorHandler.DefaultExceptionStrategy {

    @Override
    public boolean isFatal(Throwable t) {
        if (t instanceof ListenerExecutionFailedException) {
            // 可获取异常信息，进行ES日志
        }
        return super.isFatal(t);
    }

    @Override
    protected boolean isUserCauseFatal(Throwable cause) {
        if (cause instanceof RejectMessageException) {
            return true;
        } else if (cause instanceof MessageCodecException) {
            return true;
        }
        return false;
    }

}
