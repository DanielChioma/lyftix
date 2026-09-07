package com.lyftix.backend.service;

import com.lyftix.backend.dto.CheckInAnalyticsResponse;
import com.lyftix.backend.dto.CodingAnalyticsResponse;
import com.lyftix.backend.dto.DailyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.GitHubAnalyticsResponse;
import com.lyftix.backend.dto.MonthlyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WeeklyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WorkoutAnalyticsResponse;
import com.lyftix.backend.exception.InvalidAnalyticsDateRangeException;
import com.lyftix.backend.repository.CodingSessionRepository;
import com.lyftix.backend.repository.DailyCheckInRepository;
import com.lyftix.backend.repository.GitHubActivityRepository;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final WorkoutMetricRepository workoutMetricRepository;
    private final GitHubActivityRepository gitHubActivityRepository;
    private final CodingSessionRepository codingSessionRepository;
    private final DailyCheckInRepository dailyCheckInRepository;

    public AnalyticsService(
            WorkoutMetricRepository workoutMetricRepository,
            GitHubActivityRepository gitHubActivityRepository,
            CodingSessionRepository codingSessionRepository,
            DailyCheckInRepository dailyCheckInRepository
    ) {
        this.workoutMetricRepository = workoutMetricRepository;
        this.gitHubActivityRepository = gitHubActivityRepository;
        this.codingSessionRepository = codingSessionRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
    }

    public WorkoutAnalyticsResponse getWorkoutAnalytics(LocalDate startDate, LocalDate endDate) {
        InstantRange range = toInstantRange(startDate, endDate);
        WorkoutMetricRepository.WorkoutAggregate aggregate = workoutMetricRepository.aggregateAnalytics(
                range.startInclusive(), range.endExclusive()
        );
        return new WorkoutAnalyticsResponse(
                startDate,
                endDate,
                valueOrZero(aggregate.getTotalWorkouts()),
                valueOrZero(aggregate.getTotalCaloriesBurned()),
                valueOrZero(aggregate.getTotalDurationSeconds()),
                aggregate.getAverageIntensity(),
                workoutMetricRepository.countByWorkoutType(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new WorkoutAnalyticsResponse.CountByWorkoutType(item.getName(), item.getCount()))
                        .toList(),
                workoutMetricRepository.aggregateByDay(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new WorkoutAnalyticsResponse.DailyWorkoutMetrics(
                                item.getDate(), item.getCount(), item.getCaloriesBurned(), item.getDurationSeconds()))
                        .toList()
        );
    }

    public GitHubAnalyticsResponse getGitHubAnalytics(LocalDate startDate, LocalDate endDate) {
        InstantRange range = toInstantRange(startDate, endDate);
        return new GitHubAnalyticsResponse(
                startDate,
                endDate,
                valueOrZero(gitHubActivityRepository.countForAnalytics(range.startInclusive(), range.endExclusive())),
                gitHubActivityRepository.countByActivityType(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new GitHubAnalyticsResponse.CountByActivityType(item.getName(), item.getCount()))
                        .toList(),
                gitHubActivityRepository.countByRepository(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new GitHubAnalyticsResponse.CountByRepository(item.getName(), item.getCount()))
                        .toList(),
                gitHubActivityRepository.countByDay(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new GitHubAnalyticsResponse.DailyActivityCount(item.getDate(), item.getCount()))
                        .toList()
        );
    }

    public CodingAnalyticsResponse getCodingAnalytics(LocalDate startDate, LocalDate endDate) {
        InstantRange range = toInstantRange(startDate, endDate);
        CodingSessionRepository.CodingAggregate aggregate = codingSessionRepository.aggregateAnalytics(
                range.startInclusive(), range.endExclusive()
        );
        return new CodingAnalyticsResponse(
                startDate,
                endDate,
                valueOrZero(aggregate.getTotalSessions()),
                valueOrZero(aggregate.getTotalDurationSeconds()),
                aggregate.getAverageDurationSeconds(),
                codingSessionRepository.durationByProject(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new CodingAnalyticsResponse.DurationByProject(item.getName(), item.getDurationSeconds()))
                        .toList(),
                codingSessionRepository.durationByLanguage(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new CodingAnalyticsResponse.DurationByLanguage(item.getName(), item.getDurationSeconds()))
                        .toList(),
                codingSessionRepository.aggregateByDay(range.startInclusive(), range.endExclusive()).stream()
                        .map(item -> new CodingAnalyticsResponse.DailyCodingMetrics(
                                item.getDate(), item.getCount(), item.getDurationSeconds()))
                        .toList()
        );
    }

    public CheckInAnalyticsResponse getCheckInAnalytics(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        DailyCheckInRepository.CheckInAverages averages = dailyCheckInRepository.aggregateAverages(startDate, endDate);
        return new CheckInAnalyticsResponse(
                startDate,
                endDate,
                averages.getAverageMood(),
                averages.getAverageEnergy(),
                averages.getAverageFocus(),
                averages.getAverageStress(),
                averages.getAverageProductivity(),
                averages.getAverageSleepMinutes(),
                dailyCheckInRepository.findDailyTrends(startDate, endDate).stream()
                        .map(item -> new CheckInAnalyticsResponse.DailyCheckInTrend(
                                item.getDate(), item.getMood(), item.getEnergy(), item.getFocus(), item.getStress(),
                                item.getProductivity(), item.getSleepMinutes()))
                        .toList()
        );
    }

    public DailyAnalyticsSummaryResponse getDailySummary(LocalDate startDate, LocalDate endDate) {
        InstantRange range = toInstantRange(startDate, endDate);
        Map<LocalDate, MutableDailySummary> summaries = new TreeMap<>();
        startDate.datesUntil(endDate.plusDays(1)).forEach(date -> summaries.put(date, new MutableDailySummary()));

        workoutMetricRepository.aggregateByDay(range.startInclusive(), range.endExclusive()).forEach(item -> {
            MutableDailySummary summary = summaries.get(item.getDate());
            summary.workoutCount = item.getCount();
            summary.workoutDurationSeconds = item.getDurationSeconds();
            summary.caloriesBurned = item.getCaloriesBurned();
        });
        gitHubActivityRepository.countByDay(range.startInclusive(), range.endExclusive()).forEach(item ->
                summaries.get(item.getDate()).githubActivityCount = item.getCount());
        codingSessionRepository.aggregateByDay(range.startInclusive(), range.endExclusive()).forEach(item -> {
            MutableDailySummary summary = summaries.get(item.getDate());
            summary.codingSessionCount = item.getCount();
            summary.codingDurationSeconds = item.getDurationSeconds();
        });
        dailyCheckInRepository.findDailyTrends(startDate, endDate).forEach(item -> {
            MutableDailySummary summary = summaries.get(item.getDate());
            summary.mood = item.getMood();
            summary.energy = item.getEnergy();
            summary.focus = item.getFocus();
            summary.stress = item.getStress();
            summary.productivity = item.getProductivity();
            summary.sleepMinutes = item.getSleepMinutes();
        });

        return new DailyAnalyticsSummaryResponse(
                startDate,
                endDate,
                summaries.entrySet().stream().map(entry -> entry.getValue().toResponse(entry.getKey())).toList()
        );
    }

    public WeeklyAnalyticsSummaryResponse getWeeklySummary(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, MutablePeriodSummary> summaries = aggregatePeriods(Period.WEEK, startDate, endDate);
        List<WeeklyAnalyticsSummaryResponse.WeeklySummary> weekly = summaries.entrySet().stream()
                .map(entry -> entry.getValue().toWeeklyResponse(entry.getKey()))
                .toList();
        return new WeeklyAnalyticsSummaryResponse(startDate, endDate, weekly);
    }

    public MonthlyAnalyticsSummaryResponse getMonthlySummary(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, MutablePeriodSummary> summaries = aggregatePeriods(Period.MONTH, startDate, endDate);
        List<MonthlyAnalyticsSummaryResponse.MonthlySummary> monthly = summaries.entrySet().stream()
                .map(entry -> entry.getValue().toMonthlyResponse(entry.getKey()))
                .toList();
        return new MonthlyAnalyticsSummaryResponse(startDate, endDate, monthly);
    }

    private Map<LocalDate, MutablePeriodSummary> aggregatePeriods(
            Period period, LocalDate startDate, LocalDate endDate
    ) {
        InstantRange range = toInstantRange(startDate, endDate);
        Map<LocalDate, MutablePeriodSummary> summaries = new TreeMap<>();
        for (LocalDate bucket = period.bucketStart(startDate); !bucket.isAfter(endDate); bucket = period.next(bucket)) {
            summaries.put(bucket, new MutablePeriodSummary());
        }

        workoutMetricRepository.aggregateByPeriod(
                period.queryValue, range.startInclusive(), range.endExclusive()
        ).forEach(item -> {
            MutablePeriodSummary summary = summaries.get(item.getPeriodStart());
            summary.workoutCount = item.getCount();
            summary.workoutDurationSeconds = item.getDurationSeconds();
            summary.caloriesBurned = item.getCaloriesBurned();
            summary.averageWorkoutIntensity = item.getAverageIntensity();
        });
        gitHubActivityRepository.countByPeriod(
                period.queryValue, range.startInclusive(), range.endExclusive()
        ).forEach(item -> summaries.get(item.getPeriodStart()).githubActivityCount = item.getCount());
        codingSessionRepository.aggregateByPeriod(
                period.queryValue, range.startInclusive(), range.endExclusive()
        ).forEach(item -> {
            MutablePeriodSummary summary = summaries.get(item.getPeriodStart());
            summary.codingSessionCount = item.getCount();
            summary.codingDurationSeconds = item.getDurationSeconds();
            summary.averageCodingSessionDurationSeconds = item.getAverageDurationSeconds();
        });
        dailyCheckInRepository.aggregateByPeriod(period.queryValue, startDate, endDate).forEach(item -> {
            MutablePeriodSummary summary = summaries.get(item.getPeriodStart());
            summary.averageMood = item.getAverageMood();
            summary.averageEnergy = item.getAverageEnergy();
            summary.averageFocus = item.getAverageFocus();
            summary.averageStress = item.getAverageStress();
            summary.averageProductivity = item.getAverageProductivity();
            summary.averageSleepMinutes = item.getAverageSleepMinutes();
        });
        return summaries;
    }

    InstantRange toInstantRange(LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return new InstantRange(
                startDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
                endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        );
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new InvalidAnalyticsDateRangeException("startDate and endDate must be provided together");
        }
        if (startDate == null) {
            throw new InvalidAnalyticsDateRangeException("startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new InvalidAnalyticsDateRangeException("startDate must be on or before endDate");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    record InstantRange(Instant startInclusive, Instant endExclusive) {}

    private static class MutableDailySummary {
        private long workoutCount;
        private long workoutDurationSeconds;
        private long caloriesBurned;
        private long githubActivityCount;
        private long codingSessionCount;
        private long codingDurationSeconds;
        private Integer mood;
        private Integer energy;
        private Integer focus;
        private Integer stress;
        private Integer productivity;
        private Integer sleepMinutes;

        private DailyAnalyticsSummaryResponse.DailySummary toResponse(LocalDate date) {
            return new DailyAnalyticsSummaryResponse.DailySummary(
                    date, workoutCount, workoutDurationSeconds, caloriesBurned, githubActivityCount,
                    codingSessionCount, codingDurationSeconds, mood, energy, focus, stress, productivity, sleepMinutes
            );
        }
    }

    private enum Period {
        WEEK("week") {
            @Override LocalDate bucketStart(LocalDate date) {
                return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            }
            @Override LocalDate next(LocalDate bucketStart) { return bucketStart.plusWeeks(1); }
        },
        MONTH("month") {
            @Override LocalDate bucketStart(LocalDate date) { return date.withDayOfMonth(1); }
            @Override LocalDate next(LocalDate bucketStart) { return bucketStart.plusMonths(1); }
        };

        private final String queryValue;

        Period(String queryValue) {
            this.queryValue = queryValue;
        }

        abstract LocalDate bucketStart(LocalDate date);
        abstract LocalDate next(LocalDate bucketStart);
    }

    private static class MutablePeriodSummary {
        private long workoutCount;
        private long workoutDurationSeconds;
        private long caloriesBurned;
        private Double averageWorkoutIntensity;
        private long githubActivityCount;
        private long codingSessionCount;
        private long codingDurationSeconds;
        private Double averageCodingSessionDurationSeconds;
        private Double averageMood;
        private Double averageEnergy;
        private Double averageFocus;
        private Double averageStress;
        private Double averageProductivity;
        private Double averageSleepMinutes;

        private WeeklyAnalyticsSummaryResponse.WeeklySummary toWeeklyResponse(LocalDate periodStart) {
            return new WeeklyAnalyticsSummaryResponse.WeeklySummary(
                    periodStart, periodStart.plusDays(6), workoutCount, workoutDurationSeconds, caloriesBurned,
                    averageWorkoutIntensity, githubActivityCount, codingSessionCount, codingDurationSeconds,
                    averageCodingSessionDurationSeconds, averageMood, averageEnergy, averageFocus, averageStress,
                    averageProductivity, averageSleepMinutes
            );
        }

        private MonthlyAnalyticsSummaryResponse.MonthlySummary toMonthlyResponse(LocalDate periodStart) {
            return new MonthlyAnalyticsSummaryResponse.MonthlySummary(
                    periodStart.getYear(), periodStart.getMonthValue(), periodStart,
                    periodStart.with(TemporalAdjusters.lastDayOfMonth()), workoutCount, workoutDurationSeconds,
                    caloriesBurned, averageWorkoutIntensity, githubActivityCount, codingSessionCount,
                    codingDurationSeconds, averageCodingSessionDurationSeconds, averageMood, averageEnergy,
                    averageFocus, averageStress, averageProductivity, averageSleepMinutes
            );
        }
    }
}
