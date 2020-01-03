package com.allen.message.utils.threadpool;

public class ThreadPoolException extends RuntimeException {
    private static final long serialVersionUID = -5044860838149070093L;

    public enum ErrorCode {
        INVALID_MAX_POOL_SIZE("maxPoolSize should be greater than 0"),
        INVALID_CORE_POOL_SIZE("corePoolSize should be greater than 0 and less than maxPoolSize"),
        INVALID_QUEUE_CAPACITY("queueCapacity should be no less than 0"),
        INVALID_PRIORITY("priority should be between 1 and 10"),
        EXCEED_MAXIMUM_QUOTA("running out of thread pools"),
        TASK_REJECTED("task was rejected due to full threadpool");

        private ErrorCode(String desc) {
            this.description = desc;
        }

        private String description;

        public String getDescription() {
            return description;
        }
    }

    private final ErrorCode errorCode;

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public ThreadPoolException(ErrorCode code, String extraInfo) {
        super(code.getDescription() + ", " + extraInfo);
        this.errorCode = code;
    }

    public ThreadPoolException(ErrorCode code) {
        super(code.getDescription());
        this.errorCode = code;
    }
}

