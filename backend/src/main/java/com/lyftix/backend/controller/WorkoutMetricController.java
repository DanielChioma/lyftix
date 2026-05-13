package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CreateWorkoutMetricRequest;
import com.lyftix.backend.dto.WorkoutMetricResponse;
import com.lyftix.backend.service.WorkoutMetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;

import java.time.Instant;

import java.util.List;


@RestController
@RequestMapping("/api/workouts")
public class WorkoutMetricController {

    private final WorkoutMetricService workoutMetricService;

    public WorkoutMetricController(WorkoutMetricService workoutMetricService) {
        this.workoutMetricService = workoutMetricService;
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutMetricResponse createWorkoutMetric(
            @Valid @RequestBody CreateWorkoutMetricRequest request
    ) {
        return workoutMetricService.createWorkoutMetric(request);
    }

    @GetMapping
    public List<WorkoutMetricResponse> getAllWorkoutMetrics() {
        return workoutMetricService.getAllWorkoutMetrics();
    }

    @GetMapping("/paged")
    public Page<WorkoutMetricResponse> getWorkoutMetrics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy
    ) {
        return workoutMetricService.getWorkoutMetrics(page, size, sortBy);
    }

    @GetMapping("/filter")
    public Page<WorkoutMetricResponse> getWorkoutMetricsByDateRange(
            @RequestParam Instant start,
            @RequestParam Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
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
