package com.allen.message.exception;

/**
 * 
 * 扩展致命异常
 * <p>
 * 处理消息抛出此异常时,消息会直接进入死信队列中。
 * </p>
 *
 * @author Frank 平台架构部
 * @date 2017年7月19日
 * @version V3.0.0
 */
public class RejectMessageException extends RuntimeException {

    private static final long serialVersionUID = -175723523634549581L;


    public RejectMessageException(String message) {
        super(message);
    }


    public RejectMessageException() {
        super();
    }

    public RejectMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public RejectMessageException(Throwable cause) {
        super(cause);
    }
}
