package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateGitHubActivityRequest;
import com.lyftix.backend.dto.GitHubActivityResponse;
import com.lyftix.backend.exception.DuplicateGitHubActivityException;
import com.lyftix.backend.exception.InvalidGitHubActivityFilterException;
import com.lyftix.backend.model.GitHubActivity;
import com.lyftix.backend.repository.GitHubActivityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.hibernate.exception.ConstraintViolationException;

import java.time.Instant;
import java.util.List;

@Service
public class GitHubActivityService {

    private static final String EXTERNAL_ID_UNIQUE_CONSTRAINT = "uk_github_activity_external_id";

    private final GitHubActivityRepository gitHubActivityRepository;

    public GitHubActivityService(GitHubActivityRepository gitHubActivityRepository) {
        this.gitHubActivityRepository = gitHubActivityRepository;
    }

    public GitHubActivityResponse createGitHubActivity(CreateGitHubActivityRequest request) {
        GitHubActivity activity = new GitHubActivity();
        activity.setActivityType(request.activityType());
        activity.setRepositoryName(request.repositoryName());
        activity.setRepositoryOwner(request.repositoryOwner());
        activity.setOccurredAt(request.occurredAt());
        activity.setExternalId(request.externalId());
        activity.setTitle(request.title());

        try {
            return toResponse(gitHubActivityRepository.save(activity));
        } catch (DataIntegrityViolationException exception) {
            if (isExternalIdUniqueConstraintViolation(exception)) {
                throw new DuplicateGitHubActivityException(request.externalId());
            }
            throw exception;
        }
    }

    public List<GitHubActivityResponse> getAllGitHubActivities() {
        return gitHubActivityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<GitHubActivityResponse> getGitHubActivities(int page, int size, String sortBy) {
        return gitHubActivityRepository.findAll(pageable(page, size, sortBy))
                .map(this::toResponse);
    }

    public Page<GitHubActivityResponse> filterGitHubActivities(
            String activityType,
            Instant start,
            Instant end,
            int page,
            int size,
            String sortBy
    ) {
        validateFilters(activityType, start, end);

        Pageable pageable = pageable(page, size, sortBy);
        boolean hasActivityType = activityType != null;
        boolean hasDateRange = start != null;

        if (hasActivityType && hasDateRange) {
            return gitHubActivityRepository
                    .findByActivityTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(activityType, start, end, pageable)
                    .map(this::toResponse);
        }
        if (hasActivityType) {
            return gitHubActivityRepository.findByActivityType(activityType, pageable)
                    .map(this::toResponse);
        }
        if (hasDateRange) {
            return gitHubActivityRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(start, end, pageable)
                    .map(this::toResponse);
        }
        return gitHubActivityRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private void validateFilters(String activityType, Instant start, Instant end) {
        if (activityType != null && activityType.isBlank()) {
            throw new InvalidGitHubActivityFilterException("activityType must not be blank");
        }
        if ((start == null) != (end == null)) {
            throw new InvalidGitHubActivityFilterException("start and end must be provided together");
        }
        if (start != null && !start.isBefore(end)) {
            throw new InvalidGitHubActivityFilterException("start must be before end");
        }
    }

    private Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(page, size, Sort.by(sortBy).descending());
    }

    private boolean isExternalIdUniqueConstraintViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation
                    && EXTERNAL_ID_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private GitHubActivityResponse toResponse(GitHubActivity activity) {
        return new GitHubActivityResponse(
                activity.getId(),
                activity.getActivityType(),
                activity.getRepositoryName(),
                activity.getRepositoryOwner(),
                activity.getOccurredAt(),
                activity.getExternalId(),
                activity.getTitle(),
                activity.getCreatedAt(),
                activity.getUpdatedAt()
        );
    }
}
