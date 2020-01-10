package com.alex.message.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ErrorHandler;

/**
 * 公共的错误处理器
 */
public class DefaultCustomErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCustomErrorHandler.class);

    @Override
    public void handleError(Throwable t) {
        LOGGER.error("Error Occured:", t);
    }

}
