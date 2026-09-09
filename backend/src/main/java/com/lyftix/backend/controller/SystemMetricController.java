package com.lyftix.backend.controller;

import com.lyftix.backend.dto.CreateSystemMetricRequest;
import com.lyftix.backend.dto.SystemMetricResponse;
import com.lyftix.backend.exception.ApiErrorResponse;
import com.lyftix.backend.service.SystemMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@RequestMapping("/api/system-metrics")
@Tag(name = "System Metrics", description = "Persist and retrieve host observability snapshots")
@SecurityRequirement(name = "sessionCookie")
public class SystemMetricController {

    private final SystemMetricService service;

    public SystemMetricController(SystemMetricService service) {
        this.service = service;
    }

    @Operation(summary = "Create a system metric snapshot",
            description = "Persists directly collected CPU, memory, disk, and load metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "System metric created", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Metric validation failed",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemMetricResponse createSystemMetric(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Host metric snapshot; memory and disk quantities are bytes",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateSystemMetricRequest.class)))
            @Valid @RequestBody CreateSystemMetricRequest request
    ) {
        return service.createSystemMetric(request);
    }

    @Operation(summary = "List all system metrics", description = "Returns all snapshots newest collection time first")
    @ApiResponse(responseCode = "200", description = "System metrics returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SystemMetricResponse.class))))
    @GetMapping
    public List<SystemMetricResponse> getAllSystemMetrics() {
        return service.getAllSystemMetrics();
    }

    @Operation(summary = "List paged system metrics", description = "Returns a descending, property-sorted snapshot page")
    @ApiResponse(responseCode = "200", description = "System metric page returned", useReturnTypeSchema = true)
    @GetMapping("/paged")
    public Page<SystemMetricResponse> getSystemMetrics(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Snapshots per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "SystemMetric property used for descending sorting", example = "collectedAt")
            @RequestParam(defaultValue = "collectedAt") String sortBy
    ) {
        return service.getSystemMetrics(page, size, sortBy);
    }

    @Operation(summary = "Filter system metrics",
            description = "Filters by optional exact hostname and/or inclusive collectedAt range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Filtered system metric page returned",
                    useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "Hostname or collectedAt range is invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/filter")
    public Page<SystemMetricResponse> filterSystemMetrics(
            @Parameter(description = "Optional exact hostname", example = "lyftix-server")
            @RequestParam(required = false) String hostname,
            @Parameter(description = "Inclusive lower collectedAt boundary; requires end")
            @RequestParam(required = false) Instant start,
            @Parameter(description = "Inclusive upper collectedAt boundary; must be after start")
            @RequestParam(required = false) Instant end,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Snapshots per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "SystemMetric property used for descending sorting", example = "collectedAt")
            @RequestParam(defaultValue = "collectedAt") String sortBy
    ) {
        return service.filterSystemMetrics(hostname, start, end, page, size, sortBy);
    }
}
