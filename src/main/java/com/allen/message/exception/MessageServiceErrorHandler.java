package com.allen.message.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.listener.FatalExceptionStrategy;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException;
import org.springframework.util.ErrorHandler;

/**
 * 消息监听处理类
 *
 * @author Frank 平台架构部
 * @date 2017年7月20日
 * @version V3.0.0
 */
public class MessageServiceErrorHandler implements ErrorHandler {

    protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR

    /** 异常处理策略 */
    private final FatalExceptionStrategy exceptionStrategy;


    public MessageServiceErrorHandler() {
        this.exceptionStrategy = new DefaultExceptionStrategy();
    }


    public MessageServiceErrorHandler(FatalExceptionStrategy exceptionStrategy) {
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

        /**
         * Subclasses can override this to add custom exceptions.
         * 
         * @param cause the cause
         * @return true if the cause is fatal.
         */
        protected boolean isUserCauseFatal(Throwable cause) {
            return false;
        }

    }

}
