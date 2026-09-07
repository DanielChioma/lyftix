package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CheckInAnalyticsResponse;
import com.lyftix.backend.dto.CodingAnalyticsResponse;
import com.lyftix.backend.dto.DailyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.GitHubAnalyticsResponse;
import com.lyftix.backend.dto.MonthlyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WeeklyAnalyticsSummaryResponse;
import com.lyftix.backend.dto.WorkoutAnalyticsResponse;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Read-only derived analytics across Lyftix domains")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Operation(summary = "Get workout analytics",
            description = "Aggregates workouts whose startedAt falls in the inclusive UTC calendar-date range; durations are derived")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workout analytics returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/workouts")
    public WorkoutAnalyticsResponse getWorkoutAnalytics(
            @Parameter(description = "Inclusive start date at 00:00 UTC", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive end date through the following 00:00 UTC boundary", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getWorkoutAnalytics(startDate, endDate);
    }

    @Operation(summary = "Get GitHub activity analytics",
            description = "Aggregates events whose occurredAt falls in the inclusive UTC calendar-date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "GitHub analytics returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/github")
    public GitHubAnalyticsResponse getGitHubAnalytics(
            @Parameter(description = "Inclusive start date at 00:00 UTC", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive end date through the following 00:00 UTC boundary", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getGitHubAnalytics(startDate, endDate);
    }

    @Operation(summary = "Get coding-session analytics",
            description = "Aggregates sessions whose startedAt falls in the inclusive UTC calendar-date range; durations are derived")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Coding analytics returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/coding")
    public CodingAnalyticsResponse getCodingAnalytics(
            @Parameter(description = "Inclusive start date at 00:00 UTC", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive end date through the following 00:00 UTC boundary", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getCodingAnalytics(startDate, endDate);
    }

    @Operation(summary = "Get daily check-in analytics",
            description = "Calculates rating and sleep averages plus daily trends over an inclusive LocalDate range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check-in analytics returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/check-ins")
    public CheckInAnalyticsResponse getCheckInAnalytics(
            @Parameter(description = "Inclusive start calendar date", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive end calendar date", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getCheckInAnalytics(startDate, endDate);
    }

    @Operation(summary = "Get cross-domain daily summary",
            description = "Returns every date in the inclusive range, using zero for missing event totals and null for absent subjective metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cross-domain daily summary returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/daily-summary")
    public DailyAnalyticsSummaryResponse getDailySummary(
            @Parameter(description = "Inclusive start date at 00:00 UTC", example = "2026-09-01")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive end date through the following 00:00 UTC boundary", example = "2026-09-07")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getDailySummary(startDate, endDate);
    }

    @Operation(summary = "Get weekly cross-domain summaries",
            description = "Returns chronological ISO-week buckets from Monday through Sunday. Partial first and last weeks retain their calendar boundaries, but aggregates include only data inside the requested inclusive range. Empty intersecting weeks are returned with zero event metrics and null averages.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weekly summaries returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/weekly")
    public WeeklyAnalyticsSummaryResponse getWeeklySummary(
            @Parameter(description = "Inclusive user-range start date; may fall mid-week", example = "2026-09-02")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive user-range end date; may fall mid-week", example = "2026-09-15")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getWeeklySummary(startDate, endDate);
    }

    @Operation(summary = "Get monthly cross-domain summaries",
            description = "Returns chronological calendar-month buckets. Partial first and last months retain their calendar boundaries, but aggregates include only data inside the requested inclusive range. Empty intersecting months are returned with zero event metrics and null averages.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Monthly summaries returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Missing or reversed date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/monthly")
    public MonthlyAnalyticsSummaryResponse getMonthlySummary(
            @Parameter(description = "Inclusive user-range start date; may fall mid-month", example = "2026-08-20")
            @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Inclusive user-range end date; may fall mid-month", example = "2026-10-10")
            @RequestParam(required = false) LocalDate endDate
    ) {
        return analyticsService.getMonthlySummary(startDate, endDate);
    }
}
