package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CreateGitHubActivityRequest;
import com.lyftix.backend.dto.GitHubActivityResponse;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.GitHubActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/github-activities")
@Tag(name = "GitHub Activity", description = "Record and retrieve GitHub activity events")
public class GitHubActivityController {

    private final GitHubActivityService gitHubActivityService;

    public GitHubActivityController(GitHubActivityService gitHubActivityService) {
        this.gitHubActivityService = gitHubActivityService;
    }

    @Operation(summary = "Create a GitHub activity", description = "Validates and persists an immutable GitHub activity event")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "GitHub activity created", useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "A GitHub activity with the supplied externalId already exists",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GitHubActivityResponse createGitHubActivity(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "GitHub activity event to validate and persist",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateGitHubActivityRequest.class))
            )
            @Valid @RequestBody CreateGitHubActivityRequest request
    ) {
        return gitHubActivityService.createGitHubActivity(request);
    }

    @Operation(summary = "List all GitHub activities", description = "Returns every persisted GitHub activity event")
    @ApiResponse(
            responseCode = "200",
            description = "GitHub activities returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = GitHubActivityResponse.class)))
    )
    @GetMapping
    public List<GitHubActivityResponse> getAllGitHubActivities() {
        return gitHubActivityService.getAllGitHubActivities();
    }

    @Operation(summary = "List paged GitHub activities", description = "Returns a descending, property-sorted page of GitHub activity events")
    @ApiResponse(responseCode = "200", description = "GitHub activity page returned", useReturnTypeSchema = true)
    @GetMapping("/paged")
    public Page<GitHubActivityResponse> getGitHubActivities(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of GitHub activities per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "GitHubActivity property used for descending sorting", example = "occurredAt")
            @RequestParam(defaultValue = "occurredAt") String sortBy
    ) {
        return gitHubActivityService.getGitHubActivities(page, size, sortBy);
    }

    @Operation(
            summary = "Filter GitHub activities",
            description = "Filters by an optional exact activity type and/or a start-inclusive, end-exclusive occurredAt range"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered GitHub activity page returned", useReturnTypeSchema = true),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid activity type or date-range filter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/filter")
    public Page<GitHubActivityResponse> filterGitHubActivities(
            @Parameter(description = "Optional exact activity type", example = "PullRequestOpened")
            @RequestParam(required = false) String activityType,
            @Parameter(description = "Optional inclusive lower occurredAt boundary; must be provided with end", example = "2026-09-01T00:00:00Z")
            @RequestParam(required = false) Instant start,
            @Parameter(description = "Optional exclusive upper occurredAt boundary; must be after start", example = "2026-10-01T00:00:00Z")
            @RequestParam(required = false) Instant end,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of GitHub activities per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "GitHubActivity property used for descending sorting", example = "occurredAt")
            @RequestParam(defaultValue = "occurredAt") String sortBy
    ) {
        return gitHubActivityService.filterGitHubActivities(activityType, start, end, page, size, sortBy);
    }
}
