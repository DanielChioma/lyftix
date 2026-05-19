package com.lyftix.backend.dto;

import java.time.Instant;

public record WorkoutMetricResponse(
        Long id,
        String workoutType,
        Integer intensity,
        Integer caloriesBurned,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt

) {
}
