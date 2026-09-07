package com.lyftix.backend.repository;

import com.lyftix.backend.model.GitHubActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface GitHubActivityRepository extends JpaRepository<GitHubActivity, Long> {

    Page<GitHubActivity> findByOccurredAtBetween(Instant start, Instant end, Pageable pageable);

    Page<GitHubActivity> findByActivityType(String activityType, Pageable pageable);

    Page<GitHubActivity> findByActivityTypeAndOccurredAtBetween(
            String activityType,
            Instant start,
            Instant end,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(*) AS "totalActivities"
            FROM github_activity
            WHERE occurred_at >= :startInclusive AND occurred_at < :endExclusive
            """, nativeQuery = true)
    Long countForAnalytics(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT activity_type AS name, COUNT(*) AS count
            FROM github_activity
            WHERE occurred_at >= :startInclusive AND occurred_at < :endExclusive
            GROUP BY activity_type ORDER BY activity_type
            """, nativeQuery = true)
    List<NamedCount> countByActivityType(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT repository_owner || '/' || repository_name AS name, COUNT(*) AS count
            FROM github_activity
            WHERE occurred_at >= :startInclusive AND occurred_at < :endExclusive
            GROUP BY repository_owner, repository_name ORDER BY name
            """, nativeQuery = true)
    List<NamedCount> countByRepository(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT (occurred_at AT TIME ZONE 'UTC')::date AS date, COUNT(*) AS count
            FROM github_activity
            WHERE occurred_at >= :startInclusive AND occurred_at < :endExclusive
            GROUP BY date ORDER BY date
            """, nativeQuery = true)
    List<DailyCount> countByDay(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    interface NamedCount {
        String getName();
        Long getCount();
    }

    interface DailyCount {
        LocalDate getDate();
        Long getCount();
    }
}
