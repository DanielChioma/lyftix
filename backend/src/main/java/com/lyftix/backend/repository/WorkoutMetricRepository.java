package com.lyftix.backend.repository;

import com.lyftix.backend.model.WorkoutMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface WorkoutMetricRepository extends JpaRepository<WorkoutMetric, Long> {

    Page<WorkoutMetric> findByStartedAtBetween(
            Instant start,
            Instant end,
            Pageable pageable
    );


}

