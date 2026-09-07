package com.lyftix.backend.exception;

public class InvalidAnalyticsDateRangeException extends RuntimeException {

    public InvalidAnalyticsDateRangeException(String message) {
        super(message);
    }
}
