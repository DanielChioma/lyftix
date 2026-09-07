package com.lyftix.backend.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "ApiErrorResponse", description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(description = "Time the error response was created", example = "2026-09-01T09:00:01Z", format = "date-time")
        Instant timestamp,
        @Schema(description = "HTTP status code", example = "400")
        Integer status,
        @Schema(description = "Error category", example = "Validation Failed")
        String error,
        @Schema(description = "Specific validation or business-rule errors", example = "[\"intensity must be less than or equal to 10\"]")
        List <String> details
) {
}
