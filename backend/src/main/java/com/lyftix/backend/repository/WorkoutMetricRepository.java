package com.lyftix.backend.repository;

import com.lyftix.backend.model.WorkoutMetric;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutMetricRepository extends JpaRepository<WorkoutMetric, Long> {

    Page<WorkoutMetric> findByStartedAtBetween(
            Instant start,
            Instant end,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(*) AS "totalWorkouts",
                   COALESCE(SUM(calories_burned), 0)::bigint AS "totalCaloriesBurned",
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "totalDurationSeconds",
                   AVG(intensity)::double precision AS "averageIntensity"
            FROM workout_metrics
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            """, nativeQuery = true)
    WorkoutAggregate aggregateAnalytics(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT workout_type AS name, COUNT(*) AS count
            FROM workout_metrics
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY workout_type ORDER BY workout_type
            """, nativeQuery = true)
    List<NamedCount> countByWorkoutType(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT (started_at AT TIME ZONE 'UTC')::date AS date, COUNT(*) AS count,
                   COALESCE(SUM(calories_burned), 0)::bigint AS "caloriesBurned",
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds"
            FROM workout_metrics
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY date ORDER BY date
            """, nativeQuery = true)
    List<WorkoutDailyAggregate> aggregateByDay(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT date_trunc(CAST(:period AS text), started_at AT TIME ZONE 'UTC')::date AS "periodStart",
                   COUNT(*) AS count,
                   COALESCE(SUM(calories_burned), 0)::bigint AS "caloriesBurned",
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds",
                   AVG(intensity)::double precision AS "averageIntensity"
            FROM workout_metrics
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY "periodStart" ORDER BY "periodStart"
            """, nativeQuery = true)
    List<WorkoutPeriodAggregate> aggregateByPeriod(
            @Param("period") String period,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    interface WorkoutAggregate {
        Long getTotalWorkouts();
        Long getTotalCaloriesBurned();
        Long getTotalDurationSeconds();
        Double getAverageIntensity();
    }

    interface NamedCount {
        String getName();
        Long getCount();
    }

    interface WorkoutDailyAggregate {
        LocalDate getDate();
        Long getCount();
        Long getCaloriesBurned();
        Long getDurationSeconds();
    }

    interface WorkoutPeriodAggregate {
        LocalDate getPeriodStart();
        Long getCount();
        Long getCaloriesBurned();
        Long getDurationSeconds();
        Double getAverageIntensity();
    }
}
