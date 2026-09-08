package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "SystemMetricResponse", description = "Persisted host metric snapshot")
public record SystemMetricResponse(
        Long id,
        String hostname,
        String source,
        @Schema(description = "Directly collected CPU utilization percentage") Double cpuPercent,
        @Schema(description = "Used physical memory in bytes") Long memoryUsedBytes,
        @Schema(description = "Total physical memory in bytes") Long memoryTotalBytes,
        @Schema(description = "Used bytes on the configured filesystem") Long diskUsedBytes,
        @Schema(description = "Total bytes on the configured filesystem") Long diskTotalBytes,
        @Schema(description = "One-minute system load average; null where unsupported", nullable = true)
        Double loadAverage1m,
        @Schema(format = "date-time") Instant collectedAt,
        @Schema(format = "date-time") Instant createdAt
) {
}
