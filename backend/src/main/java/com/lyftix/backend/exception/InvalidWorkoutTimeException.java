package com.lyftix.backend.exception;

public class InvalidWorkoutTimeException extends RuntimeException {
    public InvalidWorkoutTimeException(String message) {
        super(message);
    }
}
