package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateWorkoutMetricRequest;
import com.lyftix.backend.dto.WorkoutMetricResponse;
import com.lyftix.backend.model.WorkoutMetric;
import com.lyftix.backend.repository.WorkoutMetricRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkoutMetricService {

    private final WorkoutMetricRepository workoutMetricRepository;

    public WorkoutMetricService(WorkoutMetricRepository workoutMetricRepository) {
        this.workoutMetricRepository = workoutMetricRepository;
    }



    public WorkoutMetricResponse createWorkoutMetric(CreateWorkoutMetricRequest request) {

        WorkoutMetric workoutMetric = new WorkoutMetric();

        workoutMetric.setWorkoutType((request.workoutType()));
        workoutMetric.setIntensity(request.intensity());
        workoutMetric.setCaloriesBurned(request.caloriesBurned());
        workoutMetric.setStartedAt(request.startedAt());
        workoutMetric.setEndedAt(request.endedAt());

        WorkoutMetric savedWorkoutMetric = workoutMetricRepository.save(workoutMetric);


        return new WorkoutMetricResponse(
                savedWorkoutMetric.getId(),
                savedWorkoutMetric.getWorkoutType(),
                savedWorkoutMetric.getIntensity(),
                savedWorkoutMetric.getCaloriesBurned(),
                savedWorkoutMetric.getStartedAt(),
                savedWorkoutMetric.getEndedAt()

        );

    }

    public List<WorkoutMetricResponse> getAllWorkoutMetrics() {
        return workoutMetricRepository.findAll()
                .stream()
                .map(workoutMetric -> new WorkoutMetricResponse(
                        workoutMetric.getId(),
                        workoutMetric.getWorkoutType(),
                        workoutMetric.getIntensity(),
                        workoutMetric.getCaloriesBurned(),
                        workoutMetric.getStartedAt(),
                        workoutMetric.getEndedAt()
                ))
                .toList();
    }
}
