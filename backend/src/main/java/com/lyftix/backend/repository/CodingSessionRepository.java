package com.lyftix.backend.repository;

import com.lyftix.backend.model.CodingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface CodingSessionRepository extends JpaRepository<CodingSession, Long> {

    @Query("""
            SELECT session FROM CodingSession session
            WHERE (:projectName IS NULL OR session.projectName = :projectName)
              AND (:language IS NULL OR session.language = :language)
              AND session.startedAt >= COALESCE(:start, session.startedAt)
              AND session.startedAt <= COALESCE(:end, session.startedAt)
            """)
    Page<CodingSession> findByFilters(
            @Param("projectName") String projectName,
            @Param("language") String language,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

    @Query(value = """
            SELECT COUNT(*) AS "totalSessions",
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "totalDurationSeconds",
                   AVG(EXTRACT(EPOCH FROM ended_at - started_at))::double precision AS "averageDurationSeconds"
            FROM coding_sessions
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            """, nativeQuery = true)
    CodingAggregate aggregateAnalytics(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT project_name AS name,
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds"
            FROM coding_sessions
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY project_name ORDER BY project_name
            """, nativeQuery = true)
    List<NamedDuration> durationByProject(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT language AS name,
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds"
            FROM coding_sessions
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY language ORDER BY language
            """, nativeQuery = true)
    List<NamedDuration> durationByLanguage(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT (started_at AT TIME ZONE 'UTC')::date AS date, COUNT(*) AS count,
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds"
            FROM coding_sessions
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY date ORDER BY date
            """, nativeQuery = true)
    List<CodingDailyAggregate> aggregateByDay(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(value = """
            SELECT date_trunc(CAST(:period AS text), started_at AT TIME ZONE 'UTC')::date AS "periodStart",
                   COUNT(*) AS count,
                   COALESCE(SUM(EXTRACT(EPOCH FROM ended_at - started_at)), 0)::bigint AS "durationSeconds",
                   AVG(EXTRACT(EPOCH FROM ended_at - started_at))::double precision AS "averageDurationSeconds"
            FROM coding_sessions
            WHERE started_at >= :startInclusive AND started_at < :endExclusive
            GROUP BY "periodStart" ORDER BY "periodStart"
            """, nativeQuery = true)
    List<CodingPeriodAggregate> aggregateByPeriod(
            @Param("period") String period,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    interface CodingAggregate {
        Long getTotalSessions();
        Long getTotalDurationSeconds();
        Double getAverageDurationSeconds();
    }

    interface NamedDuration {
        String getName();
        Long getDurationSeconds();
    }

    interface CodingDailyAggregate {
        LocalDate getDate();
        Long getCount();
        Long getDurationSeconds();
    }

    interface CodingPeriodAggregate {
        LocalDate getPeriodStart();
        Long getCount();
        Long getDurationSeconds();
        Double getAverageDurationSeconds();
    }
}
