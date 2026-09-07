package com.lyftix.backend.exception;

public class InvalidDailyCheckInFilterException extends RuntimeException {

    public InvalidDailyCheckInFilterException(String message) {
        super(message);
    }
}
