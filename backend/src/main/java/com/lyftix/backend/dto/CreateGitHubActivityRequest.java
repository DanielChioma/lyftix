package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "CreateGitHubActivityRequest", description = "Values required to record an immutable GitHub activity event")
public record CreateGitHubActivityRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "GitHub event type", example = "PullRequestOpened", maxLength = 50)
        String activityType,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Repository name", example = "lyftix", maxLength = 255)
        String repositoryName,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Repository owner or organization", example = "octocat", maxLength = 255)
        String repositoryOwner,

        @NotNull
        @Schema(description = "Time the GitHub event occurred", example = "2026-09-01T12:00:00Z", format = "date-time")
        Instant occurredAt,

        @NotBlank
        @Size(max = 255)
        @Schema(description = "Stable external event identifier used for idempotency", example = "github-event-12345", maxLength = 255)
        String externalId,

        @NotBlank
        @Size(max = 500)
        @Schema(description = "Human-readable event title", example = "Open pull request #42", maxLength = 500)
        String title
) {
}
