package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "CodingSessionResponse", description = "Persisted coding session with dynamically derived duration")
public record CodingSessionResponse(
        @Schema(description = "Generated session identifier", example = "42") Long id,
        @Schema(description = "Project being worked on", example = "lyftix") String projectName,
        @Schema(description = "Primary programming language", example = "Java") String language,
        @Schema(description = "Session start time", format = "date-time") Instant startedAt,
        @Schema(description = "Session end time", format = "date-time") Instant endedAt,
        @Schema(description = "How the session was recorded", example = "manual") String source,
        @Schema(description = "Optional session notes", nullable = true) String notes,
        @Schema(description = "Duration derived from startedAt and endedAt, in whole seconds", example = "5400") Long durationSeconds,
        @Schema(description = "Creation timestamp", format = "date-time") Instant createdAt,
        @Schema(description = "Last update timestamp", format = "date-time") Instant updatedAt
) {
}
