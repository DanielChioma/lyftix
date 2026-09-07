package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "GitHubActivityResponse", description = "Persisted GitHub activity event")
public record GitHubActivityResponse(
        @Schema(description = "Generated activity identifier", example = "42")
        Long id,
        @Schema(description = "GitHub event type", example = "PullRequestOpened")
        String activityType,
        @Schema(description = "Repository name", example = "lyftix")
        String repositoryName,
        @Schema(description = "Repository owner or organization", example = "octocat")
        String repositoryOwner,
        @Schema(description = "Time the GitHub event occurred", example = "2026-09-01T12:00:00Z", format = "date-time")
        Instant occurredAt,
        @Schema(description = "Stable external event identifier", example = "github-event-12345")
        String externalId,
        @Schema(description = "Human-readable event title", example = "Open pull request #42")
        String title,
        @Schema(description = "Creation timestamp", example = "2026-09-01T12:00:01Z", format = "date-time")
        Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-01T12:00:01Z", format = "date-time")
        Instant updatedAt
) {
}
