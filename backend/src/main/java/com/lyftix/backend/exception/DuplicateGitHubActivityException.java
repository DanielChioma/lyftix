package com.lyftix.backend.exception;

public class DuplicateGitHubActivityException extends RuntimeException {

    public DuplicateGitHubActivityException(String externalId) {
        super("GitHub activity with externalId '" + externalId + "' already exists");
    }
}
