package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "GitHub activity aggregates for an inclusive calendar-date range")
public record GitHubAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalActivities,
        List<CountByActivityType> countsByActivityType,
        List<CountByRepository> countsByRepository,
        List<DailyActivityCount> daily
) {
    public record CountByActivityType(String activityType, long count) {}
    public record CountByRepository(String repository, long count) {}
    public record DailyActivityCount(LocalDate date, long activityCount) {}
}
