package com.lyftix.backend.exception;

public class InvalidGitHubActivityFilterException extends RuntimeException {

    public InvalidGitHubActivityFilterException(String message) {
        super(message);
    }
}
