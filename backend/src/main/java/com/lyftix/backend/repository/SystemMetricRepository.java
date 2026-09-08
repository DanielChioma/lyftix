package com.lyftix.backend.repository;

import com.lyftix.backend.model.SystemMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface SystemMetricRepository extends JpaRepository<SystemMetric, Long> {

    @Query("""
            SELECT metric FROM SystemMetric metric
            WHERE (:hostname IS NULL OR metric.hostname = :hostname)
              AND metric.collectedAt >= COALESCE(:start, metric.collectedAt)
              AND metric.collectedAt <= COALESCE(:end, metric.collectedAt)
            """)
    Page<SystemMetric> findByFilters(
            @Param("hostname") String hostname,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );
}
