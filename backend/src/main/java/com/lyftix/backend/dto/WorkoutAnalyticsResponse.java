package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Workout aggregates for an inclusive calendar-date range")
public record WorkoutAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalWorkouts,
        @Schema(description = "Number of workouts with a recorded calorie value")
        long workoutsWithCalories,
        @Schema(description = "Sum of recorded calorie values; zero when none are recorded")
        long totalCaloriesBurned,
        @Schema(description = "Sum of derived workout durations in seconds") long totalDurationSeconds,
        Double averageIntensity,
        List<CountByWorkoutType> countsByWorkoutType,
        List<DailyWorkoutMetrics> daily
) {
    public record CountByWorkoutType(String workoutType, long count) {}
    public record DailyWorkoutMetrics(
            LocalDate date, long workoutCount, long workoutsWithCalories,
            long caloriesBurned, long durationSeconds
    ) {}
}
