package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(name = "CreateWorkoutMetricRequest", description = "Values required to create a workout metric")
public record CreateWorkoutMetricRequest(

        @NotBlank
        @Schema(description = "Workout activity type", example = "Running")
        String workoutType,

        @NotNull
        @Min(1)
        @Max(10)
        @Schema(description = "Workout intensity from 1 to 10", example = "7", minimum = "1", maximum = "10")
        Integer intensity,

        @NotNull
        @Min(0)
        @Schema(description = "Estimated calories burned", example = "450", minimum = "0")
        Integer caloriesBurned,

        @NotNull
        @Schema(description = "Workout start time", example = "2026-09-01T08:00:00Z", format = "date-time")
        Instant startedAt,

        @NotNull
        @Schema(description = "Workout end time; must be strictly after startedAt", example = "2026-09-01T09:00:00Z", format = "date-time")
        Instant endedAt

) {
}
