package com.lyftix.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception
    ) {

        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError ->
                        fieldError.getField() + " " + fieldError.getDefaultMessage()
                )
                .toList();

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                details
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidWorkoutTimeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWorkoutTimeException(
            InvalidWorkoutTimeException exception
    ) {

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Workout Time",
                List.of(exception.getMessage())
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(InvalidGitHubActivityFilterException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGitHubActivityFilterException(
            InvalidGitHubActivityFilterException exception
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid GitHub Activity Filter",
                List.of(exception.getMessage())
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DuplicateGitHubActivityException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateGitHubActivityException(
            DuplicateGitHubActivityException exception
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Duplicate GitHub Activity",
                List.of(exception.getMessage())
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(InvalidCodingSessionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCodingSessionException(
            InvalidCodingSessionException exception
    ) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Coding Session",
                List.of(exception.getMessage())
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
