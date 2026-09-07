package com.lyftix.backend.service;

import com.lyftix.backend.dto.CodingAnalyticsResponse;
import com.lyftix.backend.dto.DailyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.MonthlyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WeeklyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WorkoutAnalyticsResponse;
import com.lyftix.backend.exception.InvalidAnalyticsDateRangeException;
import com.lyftix.backend.repository.CodingSessionRepository;
import com.lyftix.backend.repository.DailyCheckInRepository;
import com.lyftix.backend.repository.GitHubActivityRepository;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {

    private static final LocalDate START = LocalDate.parse("2026-09-01");
    private static final LocalDate END = LocalDate.parse("2026-09-02");

    @Mock private WorkoutMetricRepository workoutRepository;
    @Mock private GitHubActivityRepository githubRepository;
    @Mock private CodingSessionRepository codingRepository;
    @Mock private DailyCheckInRepository checkInRepository;

    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsService(workoutRepository, githubRepository, codingRepository, checkInRepository);
    }

    @Test
    void convertsInclusiveCalendarDatesToUtcHalfOpenInstantRange() {
        AnalyticsService.InstantRange range = service.toInstantRange(START, END);

        assertThat(range.startInclusive()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2026-09-03T00:00:00Z"));
    }

    @Test
    void acceptsSameDayRange() {
        AnalyticsService.InstantRange range = service.toInstantRange(START, START);

        assertThat(range.startInclusive()).isEqualTo(Instant.parse("2026-09-01T00:00:00Z"));
        assertThat(range.endExclusive()).isEqualTo(Instant.parse("2026-09-02T00:00:00Z"));
    }

    @Test
    void rejectsReversedRangeBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getWorkoutAnalytics(END, START))
                .isInstanceOf(InvalidAnalyticsDateRangeException.class)
                .hasMessage("startDate must be on or before endDate");
        verifyNoInteractions(workoutRepository, githubRepository, codingRepository, checkInRepository);
    }

    @Test
    void rejectsIncompleteRangeBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getGitHubAnalytics(START, null))
                .isInstanceOf(InvalidAnalyticsDateRangeException.class)
                .hasMessage("startDate and endDate must be provided together");
        verifyNoInteractions(workoutRepository, githubRepository, codingRepository, checkInRepository);
    }

    @Test
    void mapsEmptyWorkoutAggregatesWithoutLoadingEntities() {
        WorkoutMetricRepository.WorkoutAggregate aggregate = mock(WorkoutMetricRepository.WorkoutAggregate.class);
        when(aggregate.getAverageIntensity()).thenReturn(null);
        when(workoutRepository.aggregateAnalytics(any(), any())).thenReturn(aggregate);
        when(workoutRepository.countByWorkoutType(any(), any())).thenReturn(List.of());
        when(workoutRepository.aggregateByDay(any(), any())).thenReturn(List.of());

        WorkoutAnalyticsResponse response = service.getWorkoutAnalytics(START, END);

        assertThat(response.totalWorkouts()).isZero();
        assertThat(response.totalCaloriesBurned()).isZero();
        assertThat(response.totalDurationSeconds()).isZero();
        assertThat(response.averageIntensity()).isNull();
        assertThat(response.countsByWorkoutType()).isEmpty();
        assertThat(response.daily()).isEmpty();
    }

    @Test
    void mapsWorkoutAggregateAndDerivedDuration() {
        WorkoutMetricRepository.WorkoutAggregate aggregate = mock(WorkoutMetricRepository.WorkoutAggregate.class);
        WorkoutMetricRepository.NamedCount type = mock(WorkoutMetricRepository.NamedCount.class);
        when(aggregate.getTotalWorkouts()).thenReturn(2L);
        when(aggregate.getTotalCaloriesBurned()).thenReturn(700L);
        when(aggregate.getTotalDurationSeconds()).thenReturn(5400L);
        when(aggregate.getAverageIntensity()).thenReturn(7.5);
        when(type.getName()).thenReturn("Running");
        when(type.getCount()).thenReturn(2L);
        when(workoutRepository.aggregateAnalytics(any(), any())).thenReturn(aggregate);
        when(workoutRepository.countByWorkoutType(any(), any())).thenReturn(List.of(type));
        when(workoutRepository.aggregateByDay(any(), any())).thenReturn(List.of());

        WorkoutAnalyticsResponse response = service.getWorkoutAnalytics(START, END);

        assertThat(response.totalDurationSeconds()).isEqualTo(5400);
        assertThat(response.averageIntensity()).isEqualTo(7.5);
        assertThat(response.countsByWorkoutType().getFirst().workoutType()).isEqualTo("Running");
    }

    @Test
    void mapsCodingDerivedDurationAggregates() {
        CodingSessionRepository.CodingAggregate aggregate = mock(CodingSessionRepository.CodingAggregate.class);
        when(aggregate.getTotalSessions()).thenReturn(2L);
        when(aggregate.getTotalDurationSeconds()).thenReturn(7200L);
        when(aggregate.getAverageDurationSeconds()).thenReturn(3600.0);
        when(codingRepository.aggregateAnalytics(any(), any())).thenReturn(aggregate);
        when(codingRepository.durationByProject(any(), any())).thenReturn(List.of());
        when(codingRepository.durationByLanguage(any(), any())).thenReturn(List.of());
        when(codingRepository.aggregateByDay(any(), any())).thenReturn(List.of());

        CodingAnalyticsResponse response = service.getCodingAnalytics(START, END);

        assertThat(response.totalSessions()).isEqualTo(2);
        assertThat(response.totalDurationSeconds()).isEqualTo(7200);
        assertThat(response.averageDurationSeconds()).isEqualTo(3600.0);
    }

    @Test
    void mergesCrossDomainDaysAndHandlesMissingData() {
        WorkoutMetricRepository.WorkoutDailyAggregate workout = mock(WorkoutMetricRepository.WorkoutDailyAggregate.class);
        GitHubActivityRepository.DailyCount github = mock(GitHubActivityRepository.DailyCount.class);
        CodingSessionRepository.CodingDailyAggregate coding = mock(CodingSessionRepository.CodingDailyAggregate.class);
        DailyCheckInRepository.DailyTrend checkIn = mock(DailyCheckInRepository.DailyTrend.class);
        when(workout.getDate()).thenReturn(START);
        when(workout.getCount()).thenReturn(1L);
        when(workout.getCaloriesBurned()).thenReturn(400L);
        when(workout.getDurationSeconds()).thenReturn(1800L);
        when(github.getDate()).thenReturn(END);
        when(github.getCount()).thenReturn(3L);
        when(coding.getDate()).thenReturn(START);
        when(coding.getCount()).thenReturn(2L);
        when(coding.getDurationSeconds()).thenReturn(3600L);
        when(checkIn.getDate()).thenReturn(END);
        when(checkIn.getMood()).thenReturn(8);
        when(checkIn.getEnergy()).thenReturn(7);
        when(checkIn.getFocus()).thenReturn(9);
        when(checkIn.getStress()).thenReturn(3);
        when(checkIn.getProductivity()).thenReturn(8);
        when(checkIn.getSleepMinutes()).thenReturn(480);
        when(workoutRepository.aggregateByDay(any(), any())).thenReturn(List.of(workout));
        when(githubRepository.countByDay(any(), any())).thenReturn(List.of(github));
        when(codingRepository.aggregateByDay(any(), any())).thenReturn(List.of(coding));
        when(checkInRepository.findDailyTrends(START, END)).thenReturn(List.of(checkIn));

        DailyAnalyticsSummaryResponse response = service.getDailySummary(START, END);

        assertThat(response.daily()).hasSize(2);
        DailyAnalyticsSummaryResponse.DailySummary first = response.daily().getFirst();
        assertThat(first.workoutCount()).isEqualTo(1);
        assertThat(first.githubActivityCount()).isZero();
        assertThat(first.codingDurationSeconds()).isEqualTo(3600);
        assertThat(first.mood()).isNull();
        DailyAnalyticsSummaryResponse.DailySummary second = response.daily().get(1);
        assertThat(second.workoutCount()).isZero();
        assertThat(second.githubActivityCount()).isEqualTo(3);
        assertThat(second.mood()).isEqualTo(8);
    }

    @Test
    void buildsMondayBasedIsoWeeksIncludingPartialFirstAndLastWeeks() {
        stubEmptyPeriodRepositories();

        WeeklyAnalyticsSummaryResponse response = service.getWeeklySummary(
                LocalDate.parse("2026-09-02"), LocalDate.parse("2026-09-15")
        );

        assertThat(response.weekly()).hasSize(3);
        assertThat(response.weekly().getFirst().periodStart()).isEqualTo(LocalDate.parse("2026-08-31"));
        assertThat(response.weekly().getFirst().periodEnd()).isEqualTo(LocalDate.parse("2026-09-06"));
        assertThat(response.weekly().getLast().periodStart()).isEqualTo(LocalDate.parse("2026-09-14"));
        assertThat(response.weekly().getLast().periodEnd()).isEqualTo(LocalDate.parse("2026-09-20"));
    }

    @Test
    void buildsPartialMonthsAcrossYearBoundaryInChronologicalOrder() {
        stubEmptyPeriodRepositories();

        MonthlyAnalyticsSummaryResponse response = service.getMonthlySummary(
                LocalDate.parse("2026-12-20"), LocalDate.parse("2027-02-03")
        );

        assertThat(response.monthly()).hasSize(3);
        assertThat(response.monthly().getFirst().periodStart()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(response.monthly().getFirst().periodEnd()).isEqualTo(LocalDate.parse("2026-12-31"));
        assertThat(response.monthly().getLast().year()).isEqualTo(2027);
        assertThat(response.monthly().getLast().month()).isEqualTo(2);
        assertThat(response.monthly().getLast().periodEnd()).isEqualTo(LocalDate.parse("2027-02-28"));
    }

    @Test
    void returnsEmptyPeriodsWithZeroEventsAndNullAverages() {
        stubEmptyPeriodRepositories();

        WeeklyAnalyticsSummaryResponse.WeeklySummary summary = service
                .getWeeklySummary(START, START).weekly().getFirst();

        assertThat(summary.workoutCount()).isZero();
        assertThat(summary.workoutDurationSeconds()).isZero();
        assertThat(summary.githubActivityCount()).isZero();
        assertThat(summary.codingSessionCount()).isZero();
        assertThat(summary.averageWorkoutIntensity()).isNull();
        assertThat(summary.averageMood()).isNull();
    }

    @Test
    void mapsCrossDomainPeriodAggregates() {
        WorkoutMetricRepository.WorkoutPeriodAggregate workout = mock(WorkoutMetricRepository.WorkoutPeriodAggregate.class);
        GitHubActivityRepository.PeriodCount github = mock(GitHubActivityRepository.PeriodCount.class);
        CodingSessionRepository.CodingPeriodAggregate coding = mock(CodingSessionRepository.CodingPeriodAggregate.class);
        DailyCheckInRepository.CheckInPeriodAverages checkIn = mock(DailyCheckInRepository.CheckInPeriodAverages.class);
        when(workout.getPeriodStart()).thenReturn(START);
        when(workout.getCount()).thenReturn(2L);
        when(workout.getDurationSeconds()).thenReturn(5400L);
        when(workout.getCaloriesBurned()).thenReturn(700L);
        when(workout.getAverageIntensity()).thenReturn(7.5);
        when(github.getPeriodStart()).thenReturn(START);
        when(github.getCount()).thenReturn(4L);
        when(coding.getPeriodStart()).thenReturn(START);
        when(coding.getCount()).thenReturn(3L);
        when(coding.getDurationSeconds()).thenReturn(7200L);
        when(coding.getAverageDurationSeconds()).thenReturn(2400.0);
        when(checkIn.getPeriodStart()).thenReturn(START);
        when(checkIn.getAverageMood()).thenReturn(8.0);
        when(checkIn.getAverageSleepMinutes()).thenReturn(480.0);
        when(workoutRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of(workout));
        when(githubRepository.countByPeriod(anyString(), any(), any())).thenReturn(List.of(github));
        when(codingRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of(coding));
        when(checkInRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of(checkIn));

        MonthlyAnalyticsSummaryResponse.MonthlySummary summary = service
                .getMonthlySummary(START, END).monthly().getFirst();

        assertThat(summary.workoutCount()).isEqualTo(2);
        assertThat(summary.workoutDurationSeconds()).isEqualTo(5400);
        assertThat(summary.averageWorkoutIntensity()).isEqualTo(7.5);
        assertThat(summary.githubActivityCount()).isEqualTo(4);
        assertThat(summary.codingDurationSeconds()).isEqualTo(7200);
        assertThat(summary.averageCodingSessionDurationSeconds()).isEqualTo(2400.0);
        assertThat(summary.averageMood()).isEqualTo(8.0);
        assertThat(summary.averageSleepMinutes()).isEqualTo(480.0);
    }

    private void stubEmptyPeriodRepositories() {
        when(workoutRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of());
        when(githubRepository.countByPeriod(anyString(), any(), any())).thenReturn(List.of());
        when(codingRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of());
        when(checkInRepository.aggregateByPeriod(anyString(), any(), any())).thenReturn(List.of());
    }
}
