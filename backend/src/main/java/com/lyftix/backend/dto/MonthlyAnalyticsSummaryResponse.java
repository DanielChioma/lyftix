package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Chronological calendar-month summaries intersecting the requested inclusive range")
public record MonthlyAnalyticsSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<MonthlySummary> monthly
) {
    public record MonthlySummary(
            int year,
            @Schema(description = "Calendar month number from 1 through 12") int month,
            LocalDate periodStart,
            LocalDate periodEnd,
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
