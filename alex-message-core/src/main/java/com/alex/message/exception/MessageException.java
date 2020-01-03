package com.alex.message.exception;

public class MessageException extends RuntimeException {

    private static final long serialVersionUID = -175723523634549581L;

    public MessageException(String message) {
        super(message);
    }

    public MessageException() {
        super();
    }

    public MessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageException(Throwable cause) {
        super(cause);
    }
}
