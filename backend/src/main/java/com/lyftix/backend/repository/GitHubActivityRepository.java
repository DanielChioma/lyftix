package com.lyftix.backend.repository;

import com.lyftix.backend.model.GitHubActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface GitHubActivityRepository extends JpaRepository<GitHubActivity, Long> {

    Page<GitHubActivity> findByOccurredAtBetween(Instant start, Instant end, Pageable pageable);

    Page<GitHubActivity> findByActivityType(String activityType, Pageable pageable);

    Page<GitHubActivity> findByActivityTypeAndOccurredAtBetween(
            String activityType,
            Instant start,
            Instant end,
            Pageable pageable
    );
}
