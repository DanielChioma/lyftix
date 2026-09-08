package com.lyftix.backend.exception;

public class InvalidSystemMetricException extends RuntimeException {

    public InvalidSystemMetricException(String message) {
        super(message);
    }
}
