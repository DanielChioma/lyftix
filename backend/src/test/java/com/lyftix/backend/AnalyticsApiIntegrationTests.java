package com.lyftix.backend;

import com.lyftix.backend.model.CodingSession;
import com.lyftix.backend.model.DailyCheckIn;
import com.lyftix.backend.model.GitHubActivity;
import com.lyftix.backend.model.WorkoutMetric;
import com.lyftix.backend.repository.CodingSessionRepository;
import com.lyftix.backend.repository.DailyCheckInRepository;
import com.lyftix.backend.repository.GitHubActivityRepository;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private WorkoutMetricRepository workoutRepository;
    @Autowired private GitHubActivityRepository githubRepository;
    @Autowired private CodingSessionRepository codingRepository;
    @Autowired private DailyCheckInRepository checkInRepository;

    @BeforeEach
    void cleanDatabase() {
        workoutRepository.deleteAll();
        githubRepository.deleteAll();
        codingRepository.deleteAll();
        checkInRepository.deleteAll();
    }

    @Test
    void aggregatesWorkoutTotalsTypesAndDays() throws Exception {
        saveWorkout("Running", 6, 300, "2026-09-01T09:00:00Z", "2026-09-01T09:30:00Z");
        saveWorkout("Running", 8, 500, "2026-09-02T09:00:00Z", "2026-09-02T10:00:00Z");
        saveWorkout("Cycling", 10, 900, "2026-09-03T00:00:00Z", "2026-09-03T01:00:00Z");

        mockMvc.perform(analyticsGet("/workouts", "2026-09-01", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkouts").value(2))
                .andExpect(jsonPath("$.totalCaloriesBurned").value(800))
                .andExpect(jsonPath("$.totalDurationSeconds").value(5400))
                .andExpect(jsonPath("$.averageIntensity").value(7.0))
                .andExpect(jsonPath("$.countsByWorkoutType[0].workoutType").value("Running"))
                .andExpect(jsonPath("$.countsByWorkoutType[0].count").value(2))
                .andExpect(jsonPath("$.daily.length()").value(2));
    }

    @Test
    void aggregatesGitHubActivityByTypeRepositoryAndDay() throws Exception {
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-09-01T10:00:00Z", "event-1");
        saveGitHub("IssueOpened", "lyftix", "octocat", "2026-09-01T11:00:00Z", "event-2");
        saveGitHub("PushEvent", "other", "octocat", "2026-09-02T11:00:00Z", "event-3");

        mockMvc.perform(analyticsGet("/github", "2026-09-01", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActivities").value(3))
                .andExpect(jsonPath("$.countsByActivityType.length()").value(2))
                .andExpect(jsonPath("$.countsByRepository.length()").value(2))
                .andExpect(jsonPath("$.countsByRepository[0].repository").value("octocat/lyftix"))
                .andExpect(jsonPath("$.countsByRepository[0].count").value(2))
                .andExpect(jsonPath("$.daily[0].activityCount").value(2));
    }

    @Test
    void aggregatesCodingDurationsByProjectLanguageAndDay() throws Exception {
        saveCoding("lyftix", "Java", "2026-09-01T09:00:00Z", "2026-09-01T10:00:00Z");
        saveCoding("lyftix", "Java", "2026-09-02T09:00:00Z", "2026-09-02T09:30:00Z");
        saveCoding("tools", "Python", "2026-09-02T10:00:00Z", "2026-09-02T10:30:00Z");

        mockMvc.perform(analyticsGet("/coding", "2026-09-01", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(3))
                .andExpect(jsonPath("$.totalDurationSeconds").value(7200))
                .andExpect(jsonPath("$.averageDurationSeconds").value(2400.0))
                .andExpect(jsonPath("$.durationsByProject[0].project").value("lyftix"))
                .andExpect(jsonPath("$.durationsByProject[0].durationSeconds").value(5400))
                .andExpect(jsonPath("$.durationsByLanguage.length()").value(2))
                .andExpect(jsonPath("$.daily.length()").value(2));
    }

    @Test
    void calculatesCheckInAveragesAndDailyTrends() throws Exception {
        saveCheckIn("2026-09-01", 6, 4, 8, 2, 420, 7);
        saveCheckIn("2026-09-02", 8, 6, 10, 4, 540, 9);

        mockMvc.perform(analyticsGet("/check-ins", "2026-09-01", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageMood").value(7.0))
                .andExpect(jsonPath("$.averageEnergy").value(5.0))
                .andExpect(jsonPath("$.averageFocus").value(9.0))
                .andExpect(jsonPath("$.averageStress").value(3.0))
                .andExpect(jsonPath("$.averageProductivity").value(8.0))
                .andExpect(jsonPath("$.averageSleepMinutes").value(480.0))
                .andExpect(jsonPath("$.daily.length()").value(2))
                .andExpect(jsonPath("$.daily[0].mood").value(6));
    }

    @Test
    void mergesCrossDomainDailySummaryAndKeepsMissingDataDays() throws Exception {
        saveWorkout("Running", 7, 350, "2026-09-01T09:00:00Z", "2026-09-01T09:30:00Z");
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-09-03T10:00:00Z", "summary-event");
        saveCoding("lyftix", "Java", "2026-09-01T11:00:00Z", "2026-09-01T12:00:00Z");
        saveCheckIn("2026-09-03", 8, 7, 9, 3, 480, 8);

        mockMvc.perform(analyticsGet("/daily-summary", "2026-09-01", "2026-09-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.daily.length()").value(3))
                .andExpect(jsonPath("$.daily[0].workoutCount").value(1))
                .andExpect(jsonPath("$.daily[0].codingDurationSeconds").value(3600))
                .andExpect(jsonPath("$.daily[0].mood").doesNotExist())
                .andExpect(jsonPath("$.daily[1].workoutCount").value(0))
                .andExpect(jsonPath("$.daily[1].githubActivityCount").value(0))
                .andExpect(jsonPath("$.daily[1].mood").doesNotExist())
                .andExpect(jsonPath("$.daily[2].githubActivityCount").value(1))
                .andExpect(jsonPath("$.daily[2].mood").value(8));
    }

    @Test
    void supportsSameDayUtcRangeAndExcludesFollowingMidnight() throws Exception {
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-09-01T23:59:59Z", "inside");
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-09-02T00:00:00Z", "outside");

        mockMvc.perform(analyticsGet("/github", "2026-09-01", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActivities").value(1))
                .andExpect(jsonPath("$.daily[0].date").value("2026-09-01"));
    }

    @Test
    void returnsCleanEmptyAnalytics() throws Exception {
        mockMvc.perform(analyticsGet("/workouts", "2026-09-01", "2026-09-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWorkouts").value(0))
                .andExpect(jsonPath("$.totalDurationSeconds").value(0))
                .andExpect(jsonPath("$.averageIntensity").doesNotExist())
                .andExpect(jsonPath("$.daily").isEmpty());
    }

    @Test
    void rejectsInvalidAndMissingRangesWithApiErrorResponse() throws Exception {
        mockMvc.perform(analyticsGet("/workouts", "2026-09-02", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Analytics Date Range"))
                .andExpect(jsonPath("$.details[0]").value("startDate must be on or before endDate"));

        mockMvc.perform(get("/api/analytics/github").param("startDate", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Analytics Date Range"))
                .andExpect(jsonPath("$.details[0]").value("startDate and endDate must be provided together"));
    }

    @Test
    void publishesAllAnalyticsPathsAndSchemasInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/analytics/workouts'].get").exists())
                .andExpect(jsonPath("$.paths['/api/analytics/github'].get").exists())
                .andExpect(jsonPath("$.paths['/api/analytics/coding'].get").exists())
                .andExpect(jsonPath("$.paths['/api/analytics/check-ins'].get").exists())
                .andExpect(jsonPath("$.paths['/api/analytics/daily-summary'].get").exists())
                .andExpect(jsonPath("$.components.schemas.WorkoutAnalyticsResponse").exists())
                .andExpect(jsonPath("$.components.schemas.GitHubAnalyticsResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CodingAnalyticsResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CheckInAnalyticsResponse").exists())
                .andExpect(jsonPath("$.components.schemas.DailyAnalyticsSummaryResponse").exists());
    }

    @Test
    void aggregatesIsoWeeksWithoutLeakingOutsidePartialRange() throws Exception {
        saveWorkout("Outside", 10, 900, "2026-09-01T10:00:00Z", "2026-09-01T11:00:00Z");
        saveWorkout("Running", 6, 300, "2026-09-02T10:00:00Z", "2026-09-02T10:30:00Z");
        saveWorkout("Cycling", 8, 500, "2026-09-03T10:00:00Z", "2026-09-03T11:00:00Z");
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-09-03T12:00:00Z", "weekly-event");
        saveCoding("lyftix", "Java", "2026-09-04T09:00:00Z", "2026-09-04T10:00:00Z");
        saveCheckIn("2026-09-02", 6, 7, 8, 3, 420, 7);
        saveCheckIn("2026-09-04", 8, 9, 10, 5, 540, 9);

        mockMvc.perform(analyticsGet("/weekly", "2026-09-02", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekly.length()").value(3))
                .andExpect(jsonPath("$.weekly[0].periodStart").value("2026-08-31"))
                .andExpect(jsonPath("$.weekly[0].periodEnd").value("2026-09-06"))
                .andExpect(jsonPath("$.weekly[0].workoutCount").value(2))
                .andExpect(jsonPath("$.weekly[0].workoutDurationSeconds").value(5400))
                .andExpect(jsonPath("$.weekly[0].caloriesBurned").value(800))
                .andExpect(jsonPath("$.weekly[0].averageWorkoutIntensity").value(7.0))
                .andExpect(jsonPath("$.weekly[0].githubActivityCount").value(1))
                .andExpect(jsonPath("$.weekly[0].codingSessionCount").value(1))
                .andExpect(jsonPath("$.weekly[0].codingDurationSeconds").value(3600))
                .andExpect(jsonPath("$.weekly[0].averageMood").value(7.0))
                .andExpect(jsonPath("$.weekly[1].workoutCount").value(0))
                .andExpect(jsonPath("$.weekly[1].averageMood").doesNotExist())
                .andExpect(jsonPath("$.weekly[2].periodStart").value("2026-09-14"));
    }

    @Test
    void aggregatesCalendarMonthsAcrossYearBoundaryAndKeepsEmptyMonth() throws Exception {
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-12-19T12:00:00Z", "outside-before");
        saveGitHub("PushEvent", "lyftix", "octocat", "2026-12-20T12:00:00Z", "december");
        saveGitHub("PushEvent", "lyftix", "octocat", "2027-02-03T12:00:00Z", "february");
        saveGitHub("PushEvent", "lyftix", "octocat", "2027-02-04T00:00:00Z", "outside-after");

        mockMvc.perform(analyticsGet("/monthly", "2026-12-20", "2027-02-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthly.length()").value(3))
                .andExpect(jsonPath("$.monthly[0].year").value(2026))
                .andExpect(jsonPath("$.monthly[0].month").value(12))
                .andExpect(jsonPath("$.monthly[0].periodStart").value("2026-12-01"))
                .andExpect(jsonPath("$.monthly[0].periodEnd").value("2026-12-31"))
                .andExpect(jsonPath("$.monthly[0].githubActivityCount").value(1))
                .andExpect(jsonPath("$.monthly[1].periodStart").value("2027-01-01"))
                .andExpect(jsonPath("$.monthly[1].githubActivityCount").value(0))
                .andExpect(jsonPath("$.monthly[1].averageMood").doesNotExist())
                .andExpect(jsonPath("$.monthly[2].month").value(2))
                .andExpect(jsonPath("$.monthly[2].githubActivityCount").value(1));
    }

    @Test
    void supportsSameDayPeriodSummary() throws Exception {
        saveCoding("lyftix", "Java", "2026-09-02T09:00:00Z", "2026-09-02T10:30:00Z");

        mockMvc.perform(analyticsGet("/weekly", "2026-09-02", "2026-09-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekly.length()").value(1))
                .andExpect(jsonPath("$.weekly[0].periodStart").value("2026-08-31"))
                .andExpect(jsonPath("$.weekly[0].codingSessionCount").value(1))
                .andExpect(jsonPath("$.weekly[0].codingDurationSeconds").value(5400))
                .andExpect(jsonPath("$.weekly[0].averageCodingSessionDurationSeconds").value(5400.0));
    }

    @Test
    void rejectsInvalidPeriodRangesWithExistingApiError() throws Exception {
        mockMvc.perform(analyticsGet("/monthly", "2026-09-03", "2026-09-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Analytics Date Range"))
                .andExpect(jsonPath("$.details[0]").value("startDate must be on or before endDate"));

        mockMvc.perform(get("/api/analytics/weekly").param("startDate", "2026-09-02"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Analytics Date Range"));
    }

    @Test
    void publishesWeeklyAndMonthlyOpenApiSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/analytics/weekly'].get").exists())
                .andExpect(jsonPath("$.paths['/api/analytics/monthly'].get").exists())
                .andExpect(jsonPath("$.components.schemas.WeeklyAnalyticsSummaryResponse").exists())
                .andExpect(jsonPath("$.components.schemas.MonthlyAnalyticsSummaryResponse").exists());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder analyticsGet(
            String path, String startDate, String endDate
    ) {
        return get("/api/analytics" + path).param("startDate", startDate).param("endDate", endDate);
    }

    private void saveWorkout(String type, int intensity, int calories, String start, String end) {
        WorkoutMetric workout = new WorkoutMetric();
        workout.setWorkoutType(type);
        workout.setIntensity(intensity);
        workout.setCaloriesBurned(calories);
        workout.setStartedAt(Instant.parse(start));
        workout.setEndedAt(Instant.parse(end));
        workoutRepository.save(workout);
    }

    private void saveGitHub(String type, String repository, String owner, String occurredAt, String externalId) {
        GitHubActivity activity = new GitHubActivity();
        activity.setActivityType(type);
        activity.setRepositoryName(repository);
        activity.setRepositoryOwner(owner);
        activity.setOccurredAt(Instant.parse(occurredAt));
        activity.setExternalId(externalId);
        activity.setTitle("Analytics event");
        githubRepository.save(activity);
    }

    private void saveCoding(String project, String language, String start, String end) {
        CodingSession session = new CodingSession();
        session.setProjectName(project);
        session.setLanguage(language);
        session.setStartedAt(Instant.parse(start));
        session.setEndedAt(Instant.parse(end));
        session.setSource("test");
        codingRepository.save(session);
    }

    private void saveCheckIn(
            String date, int mood, int energy, int focus, int stress, int sleepMinutes, int productivity
    ) {
        DailyCheckIn checkIn = new DailyCheckIn();
        checkIn.setCheckInDate(LocalDate.parse(date));
        checkIn.setMood(mood);
        checkIn.setEnergy(energy);
        checkIn.setFocus(focus);
        checkIn.setStress(stress);
        checkIn.setSleepMinutes(sleepMinutes);
        checkIn.setProductivity(productivity);
        checkInRepository.save(checkIn);
    }
}
