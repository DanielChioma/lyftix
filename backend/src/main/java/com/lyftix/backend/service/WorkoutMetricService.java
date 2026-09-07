package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateWorkoutMetricRequest;
import com.lyftix.backend.dto.WorkoutMetricResponse;
import com.lyftix.backend.model.WorkoutMetric;
import com.lyftix.backend.repository.WorkoutMetricRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import com.lyftix.backend.exception.InvalidWorkoutTimeException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service
public class WorkoutMetricService {

    private final WorkoutMetricRepository workoutMetricRepository;

    public WorkoutMetricService(WorkoutMetricRepository workoutMetricRepository) {
        this.workoutMetricRepository = workoutMetricRepository;
    }



    public WorkoutMetricResponse createWorkoutMetric(CreateWorkoutMetricRequest request) {

        if (!request.endedAt().isAfter(request.startedAt())) {
            throw new InvalidWorkoutTimeException(
                    "endedAt must be after startedAt"
            );
        }
        WorkoutMetric workoutMetric = new WorkoutMetric();

        workoutMetric.setWorkoutType((request.workoutType()));
        workoutMetric.setIntensity(request.intensity());
        workoutMetric.setCaloriesBurned(request.caloriesBurned());
        workoutMetric.setStartedAt(request.startedAt());
        workoutMetric.setEndedAt(request.endedAt());

        WorkoutMetric savedWorkoutMetric = workoutMetricRepository.save(workoutMetric);


        return toResponse(savedWorkoutMetric);



    }

    public List<WorkoutMetricResponse> getAllWorkoutMetrics() {
        return workoutMetricRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<WorkoutMetricResponse> getWorkoutMetrics(
            int page,
            int size,
            String sortBy
    ) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy).descending()
        );

        return workoutMetricRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<WorkoutMetricResponse> getWorkoutMetricsByDateRange(
            Instant start,
            Instant end,
            int page,
            int size,
            String sortBy
    ) {

        if (!start.isBefore(end)) {
            throw new InvalidWorkoutTimeException(
                    "start must be before end"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortBy).descending()
        );

        return workoutMetricRepository
                .findByStartedAtBetween(start, end, pageable)
                .map(this::toResponse);
    }

    private WorkoutMetricResponse toResponse(WorkoutMetric workoutMetric) {
        return new WorkoutMetricResponse(
                workoutMetric.getId(),
                workoutMetric.getWorkoutType(),
                workoutMetric.getIntensity(),
                workoutMetric.getCaloriesBurned(),
                workoutMetric.getStartedAt(),
                workoutMetric.getEndedAt(),
                workoutMetric.getCreatedAt(),
                workoutMetric.getUpdatedAt()
        );
    }

}
