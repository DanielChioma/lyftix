package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "CreateCodingSessionRequest", description = "Values required to record a coding session")
public record CreateCodingSessionRequest(
        @NotBlank @Size(max = 255)
        @Schema(description = "Project being worked on", example = "lyftix", maxLength = 255)
        String projectName,
        @NotBlank @Size(max = 100)
        @Schema(description = "Primary programming language", example = "Java", maxLength = 100)
        String language,
        @NotNull
        @Schema(description = "Session start time", example = "2026-09-01T09:00:00Z", format = "date-time")
        Instant startedAt,
        @NotNull
        @Schema(description = "Session end time; must be after startedAt", example = "2026-09-01T10:30:00Z", format = "date-time")
        Instant endedAt,
        @NotBlank @Size(max = 100)
        @Schema(description = "How the session was recorded", example = "manual", maxLength = 100)
        String source,
        @Size(max = 5000)
        @Schema(description = "Optional session notes", example = "Implemented coding-session persistence", maxLength = 5000)
        String notes
) {
}
