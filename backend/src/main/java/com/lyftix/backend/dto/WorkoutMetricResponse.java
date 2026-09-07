package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "WorkoutMetricResponse", description = "Persisted workout metric")
public record WorkoutMetricResponse(
        @Schema(description = "Generated workout metric identifier", example = "42")
        Long id,
        @Schema(description = "Workout activity type", example = "Running")
        String workoutType,
        @Schema(description = "Workout intensity from 1 to 10", example = "7")
        Integer intensity,
        @Schema(description = "Estimated calories burned", example = "450")
        Integer caloriesBurned,
        @Schema(description = "Workout start time", example = "2026-09-01T08:00:00Z", format = "date-time")
        Instant startedAt,
        @Schema(description = "Workout end time", example = "2026-09-01T09:00:00Z", format = "date-time")
        Instant endedAt,
        @Schema(description = "Creation timestamp", example = "2026-09-01T09:00:01Z", format = "date-time")
        Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-01T09:00:01Z", format = "date-time")
        Instant updatedAt

) {
}
