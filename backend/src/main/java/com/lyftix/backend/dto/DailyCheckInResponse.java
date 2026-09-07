package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "DailyCheckInResponse", description = "Persisted daily check-in")
public record DailyCheckInResponse(
        Long id,
        @Schema(format = "date", example = "2026-09-07") LocalDate checkInDate,
        @Schema(minimum = "1", maximum = "10") Integer mood,
        @Schema(minimum = "1", maximum = "10") Integer energy,
        @Schema(minimum = "1", maximum = "10") Integer focus,
        @Schema(minimum = "1", maximum = "10") Integer stress,
        @Schema(minimum = "0", maximum = "1440") Integer sleepMinutes,
        @Schema(minimum = "1", maximum = "10") Integer productivity,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
}
