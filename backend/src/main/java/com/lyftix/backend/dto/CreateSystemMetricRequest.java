package com.lyftix.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(name = "CreateSystemMetricRequest", description = "One collected host metric snapshot")
public record CreateSystemMetricRequest(
        @NotBlank @Size(max = 255)
        @Schema(description = "Host name reported by the operating system", example = "lyftix-server", maxLength = 255)
        String hostname,
        @NotBlank @Size(max = 100)
        @Schema(description = "Collector identity", example = "local", maxLength = 100)
        String source,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0")
        @Schema(description = "Directly collected CPU utilization percentage", example = "23.5", minimum = "0", maximum = "100")
        Double cpuPercent,
        @NotNull @PositiveOrZero
        @Schema(description = "Used physical memory in bytes", example = "4294967296")
        Long memoryUsedBytes,
        @NotNull @Positive
        @Schema(description = "Total physical memory in bytes", example = "8589934592")
        Long memoryTotalBytes,
        @NotNull @PositiveOrZero
        @Schema(description = "Used bytes on the configured filesystem", example = "107374182400")
        Long diskUsedBytes,
        @NotNull @Positive
        @Schema(description = "Total bytes on the configured filesystem", example = "214748364800")
        Long diskTotalBytes,
        @PositiveOrZero
        @Schema(description = "One-minute system load average; null where unsupported", example = "1.25", nullable = true)
        Double loadAverage1m,
        @NotNull
        @Schema(description = "UTC instant when the snapshot was collected", format = "date-time")
        Instant collectedAt
) {
}
