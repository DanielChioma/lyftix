package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Workout aggregates for an inclusive calendar-date range")
public record WorkoutAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalWorkouts,
        long totalCaloriesBurned,
        @Schema(description = "Sum of derived workout durations in seconds") long totalDurationSeconds,
        Double averageIntensity,
        List<CountByWorkoutType> countsByWorkoutType,
        List<DailyWorkoutMetrics> daily
) {
    public record CountByWorkoutType(String workoutType, long count) {}
    public record DailyWorkoutMetrics(
            LocalDate date, long workoutCount, long caloriesBurned, long durationSeconds
    ) {}
}
