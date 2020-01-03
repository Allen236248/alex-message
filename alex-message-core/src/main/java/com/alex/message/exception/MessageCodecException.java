package com.alex.message.exception;

/**
 * 序列化转换异常
 */
public class MessageCodecException extends RuntimeException {

    private static final long serialVersionUID = -175723523634549581L;

    public MessageCodecException() {
        super();
    }

    public MessageCodecException(String message) {
        super(message);
    }

    public MessageCodecException(Throwable cause) {
        super(cause);
    }

    public MessageCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
