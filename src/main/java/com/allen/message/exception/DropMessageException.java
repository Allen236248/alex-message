package com.allen.message.exception;

/**
 * 消息丢弃异常处理
 * <p>
 * 当用户抛出此异常时，消息系统会默认丢弃此消息
 * </p>
 *
 * @author Frank 平台架构部
 * @date 2017年7月19日
 * @version V3.0.0
 */
public class DropMessageException extends RuntimeException {

    private static final long serialVersionUID = -175723523634549581L;


    public DropMessageException(String message) {
        super(message);
    }


    public DropMessageException() {
        super();
    }

    public DropMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public DropMessageException(Throwable cause) {
        super(cause);
    }
}
