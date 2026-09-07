package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Daily check-in averages and trends for an inclusive calendar-date range")
public record CheckInAnalyticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        Double averageMood,
        Double averageEnergy,
        Double averageFocus,
        Double averageStress,
        Double averageProductivity,
        Double averageSleepMinutes,
        List<DailyCheckInTrend> daily
) {
    public record DailyCheckInTrend(
            LocalDate date,
            int mood,
            int energy,
            int focus,
            int stress,
            int productivity,
            int sleepMinutes
    ) {}
}
