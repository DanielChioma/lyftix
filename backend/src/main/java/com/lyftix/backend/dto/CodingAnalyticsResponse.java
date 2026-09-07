package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Coding-session aggregates for an inclusive calendar-date range")
public record CodingAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalSessions,
        @Schema(description = "Sum of derived session durations in seconds") long totalDurationSeconds,
        @Schema(description = "Average derived session duration in seconds") Double averageDurationSeconds,
        List<DurationByProject> durationsByProject,
        List<DurationByLanguage> durationsByLanguage,
        List<DailyCodingMetrics> daily
) {
    public record DurationByProject(String project, long durationSeconds) {}
    public record DurationByLanguage(String language, long durationSeconds) {}
    public record DailyCodingMetrics(LocalDate date, long sessionCount, long durationSeconds) {}
}
