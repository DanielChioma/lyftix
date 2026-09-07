package com.lyftix.backend.repository;

import com.lyftix.backend.model.CodingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

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
}
