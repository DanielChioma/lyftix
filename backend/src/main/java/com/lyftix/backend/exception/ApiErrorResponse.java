package com.lyftix.backend.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        List <String> details
) {
}
