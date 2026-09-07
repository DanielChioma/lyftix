package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Chronological ISO-week summaries intersecting the requested inclusive range")
public record WeeklyAnalyticsSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<WeeklySummary> weekly
) {
    public record WeeklySummary(
            @Schema(description = "Monday of the ISO week") LocalDate periodStart,
            @Schema(description = "Sunday of the ISO week") LocalDate periodEnd,
            long workoutCount,
            long workoutDurationSeconds,
            long caloriesBurned,
            Double averageWorkoutIntensity,
            long githubActivityCount,
            long codingSessionCount,
            long codingDurationSeconds,
            Double averageCodingSessionDurationSeconds,
            Double averageMood,
            Double averageEnergy,
            Double averageFocus,
            Double averageStress,
            Double averageProductivity,
            Double averageSleepMinutes
    ) {}
}
