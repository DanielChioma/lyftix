package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "CreateDailyCheckInRequest", description = "Subjective wellbeing and productivity signals for one calendar date")
public record CreateDailyCheckInRequest(
        @NotNull
        @Schema(description = "Calendar date of the check-in", example = "2026-09-07", format = "date")
        LocalDate checkInDate,
        @NotNull @Min(1) @Max(10)
        @Schema(description = "Mood rating", example = "8", minimum = "1", maximum = "10")
        Integer mood,
        @NotNull @Min(1) @Max(10)
        @Schema(description = "Energy rating", example = "7", minimum = "1", maximum = "10")
        Integer energy,
        @NotNull @Min(1) @Max(10)
        @Schema(description = "Focus rating", example = "9", minimum = "1", maximum = "10")
        Integer focus,
        @NotNull @Min(1) @Max(10)
        @Schema(description = "Stress rating", example = "3", minimum = "1", maximum = "10")
        Integer stress,
        @NotNull @Min(0) @Max(1440)
        @Schema(description = "Sleep duration in minutes", example = "480", minimum = "0", maximum = "1440")
        Integer sleepMinutes,
        @NotNull @Min(1) @Max(10)
        @Schema(description = "Productivity rating", example = "8", minimum = "1", maximum = "10")
        Integer productivity,
        @Size(max = 5000)
        @Schema(description = "Optional check-in notes", example = "Good focus throughout the day", maxLength = 5000)
        String notes
) {
}
