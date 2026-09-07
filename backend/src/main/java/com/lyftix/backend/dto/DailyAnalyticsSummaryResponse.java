package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Cross-domain daily aggregates for an inclusive calendar-date range")
public record DailyAnalyticsSummaryResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<DailySummary> daily
) {
    public record DailySummary(
            LocalDate date,
            long workoutCount,
            long workoutDurationSeconds,
            long caloriesBurned,
            long githubActivityCount,
            long codingSessionCount,
            long codingDurationSeconds,
            Integer mood,
            Integer energy,
            Integer focus,
            Integer stress,
            Integer productivity,
            Integer sleepMinutes
    ) {}
}
