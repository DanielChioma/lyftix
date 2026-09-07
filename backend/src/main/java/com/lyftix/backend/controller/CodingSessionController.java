package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CodingSessionResponse;
import com.lyftix.backend.dto.CreateCodingSessionRequest;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.CodingSessionService;
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
@RequestMapping("/api/coding-sessions")
@Tag(name = "Coding Sessions", description = "Record and retrieve coding sessions")
public class CodingSessionController {

    private final CodingSessionService codingSessionService;

    public CodingSessionController(CodingSessionService codingSessionService) {
        this.codingSessionService = codingSessionService;
    }

    @Operation(summary = "Create a coding session", description = "Validates and persists a coding session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Coding session created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Validation or session time failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CodingSessionResponse createCodingSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Coding session to validate and persist",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateCodingSessionRequest.class)))
            @Valid @RequestBody CreateCodingSessionRequest request
    ) {
        return codingSessionService.createCodingSession(request);
    }

    @Operation(summary = "List all coding sessions", description = "Returns every persisted coding session")
    @ApiResponse(responseCode = "200", description = "Coding sessions returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CodingSessionResponse.class))))
    @GetMapping
    public List<CodingSessionResponse> getAllCodingSessions() {
        return codingSessionService.getAllCodingSessions();
    }

    @Operation(summary = "List paged coding sessions", description = "Returns a descending, property-sorted page of coding sessions")
    @ApiResponse(responseCode = "200", description = "Coding-session page returned", useReturnTypeSchema = true)
    @GetMapping("/paged")
    public Page<CodingSessionResponse> getCodingSessions(
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sessions per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "CodingSession property used for descending sorting", example = "startedAt")
            @RequestParam(defaultValue = "startedAt") String sortBy
    ) {
        return codingSessionService.getCodingSessions(page, size, sortBy);
    }

    @Operation(summary = "Filter coding sessions",
            description = "Filters by optional exact project, language, and/or startedAt range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered coding-session page returned", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Invalid filter",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/filter")
    public Page<CodingSessionResponse> filterCodingSessions(
            @Parameter(description = "Optional exact project name", example = "lyftix")
            @RequestParam(required = false) String projectName,
            @Parameter(description = "Optional exact language", example = "Java")
            @RequestParam(required = false) String language,
            @Parameter(description = "Optional inclusive lower startedAt boundary; requires end")
            @RequestParam(required = false) Instant start,
            @Parameter(description = "Optional inclusive upper startedAt boundary; must be after start")
            @RequestParam(required = false) Instant end,
            @Parameter(description = "Zero-based page index", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sessions per page", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "CodingSession property used for descending sorting", example = "startedAt")
            @RequestParam(defaultValue = "startedAt") String sortBy
    ) {
        return codingSessionService.filterCodingSessions(projectName, language, start, end, page, size, sortBy);
    }
}
