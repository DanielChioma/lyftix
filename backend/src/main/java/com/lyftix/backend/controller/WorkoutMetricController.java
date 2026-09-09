package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CreateWorkoutMetricRequest;
import com.lyftix.backend.dto.WorkoutMetricResponse;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.WorkoutMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.time.Instant;

import java.util.List;


@RestController
@RequestMapping("/api/workouts")
@Tag(name = "Workout Metrics", description = "Create and retrieve workout metrics")
public class WorkoutMetricController {

    private final WorkoutMetricService workoutMetricService;

    public WorkoutMetricController(WorkoutMetricService workoutMetricService) {
        this.workoutMetricService = workoutMetricService;
    }
    @Operation(summary = "Create a workout metric", description = "Validates and persists a new workout metric")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Workout metric created", useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation or workout time validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutMetricResponse createWorkoutMetric(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Workout metric to validate and persist",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateWorkoutMetricRequest.class))
            )
            @Valid @RequestBody CreateWorkoutMetricRequest request
    ) {
        return workoutMetricService.createWorkoutMetric(request);
    }

    @Operation(summary = "List all workout metrics", description = "Returns every persisted workout metric")
    @ApiResponse(
            responseCode = "200",
            description = "Workout metrics returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = WorkoutMetricResponse.class)))
    )
    @GetMapping
    public List<WorkoutMetricResponse> getAllWorkoutMetrics() {
        return workoutMetricService.getAllWorkoutMetrics();
    }

    @Operation(summary = "List paged workout metrics", description = "Returns a descending, property-sorted page of workout metrics")
    @ApiResponse(responseCode = "200", description = "Workout metric page returned", useReturnTypeSchema = true)
    @GetMapping("/paged")
    public Page<WorkoutMetricResponse> getWorkoutMetrics(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of workout metrics per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "WorkoutMetric property used for descending sorting", example = "startedAt")
            @RequestParam(defaultValue = "startedAt") String sortBy
    ) {
        return workoutMetricService.getWorkoutMetrics(page, size, sortBy);
    }

    @Operation(summary = "Filter workout metrics by date", description = "Returns a page of workout metrics whose startedAt is within the start-inclusive, end-exclusive range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered workout metric page returned", useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date range, pagination, or sorting parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/filter")
    public Page<WorkoutMetricResponse> getWorkoutMetricsByDateRange(
            @Parameter(description = "Inclusive startedAt boundary in ISO-8601 format", example = "2026-09-01T00:00:00Z")
            @RequestParam Instant start,
            @Parameter(description = "Exclusive startedAt boundary in ISO-8601 format; must be after start", example = "2026-10-01T00:00:00Z")
            @RequestParam Instant end,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of workout metrics per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "WorkoutMetric property used for descending sorting", example = "startedAt")
            @RequestParam(defaultValue = "startedAt") String sortBy
    ) {
        return workoutMetricService.getWorkoutMetricsByDateRange(
                start,
                end,
                page,
                size,
                sortBy
        );
    }


}
