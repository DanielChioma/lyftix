package com.lyftix.backend.repository;

import com.lyftix.backend.model.WorkoutMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutMetricRepository extends JpaRepository<WorkoutMetric, Long> {
}

