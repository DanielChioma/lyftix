package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateSystemMetricRequest;
import com.lyftix.backend.dto.SystemMetricResponse;
import com.lyftix.backend.exception.InvalidSystemMetricException;
import com.lyftix.backend.model.SystemMetric;
import com.lyftix.backend.repository.SystemMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemMetricServiceTests {

    private static final Instant COLLECTED_AT = Instant.parse("2026-09-08T12:00:00Z");

    @Mock private SystemMetricRepository repository;
    private SystemMetricService service;

    @BeforeEach
    void setUp() {
        service = new SystemMetricService(repository);
    }

    @Test
    void createsAndMapsValidMetric() {
        SystemMetric saved = savedMetric();
        when(repository.save(any(SystemMetric.class))).thenReturn(saved);

        SystemMetricResponse response = service.createSystemMetric(validRequest());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.hostname()).isEqualTo("lyftix-server");
        assertThat(response.cpuPercent()).isEqualTo(23.5);
        assertThat(response.memoryUsedBytes()).isEqualTo(400L);
        assertThat(response.loadAverage1m()).isEqualTo(1.25);
        assertThat(response.createdAt()).isEqualTo(COLLECTED_AT.plusSeconds(1));
        verify(repository).save(any(SystemMetric.class));
    }

    @Test
    void rejectsUsedBytesAboveTotals() {
        CreateSystemMetricRequest invalid = new CreateSystemMetricRequest(
                "host", "local", 10.0, 101L, 100L, 20L, 100L, null, COLLECTED_AT
        );

        assertThatThrownBy(() -> service.createSystemMetric(invalid))
                .isInstanceOf(InvalidSystemMetricException.class)
                .hasMessage("memoryUsedBytes must not exceed memoryTotalBytes");
        verifyNoInteractions(repository);
    }

    @Test
    void routesHostnameAndInstantFilters() {
        Instant end = COLLECTED_AT.plusSeconds(60);
        when(repository.findByFilters(any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.filterSystemMetrics("lyftix-server", COLLECTED_AT, end, 0, 10, "collectedAt");

        verify(repository).findByFilters(
                eq("lyftix-server"), eq(COLLECTED_AT), eq(end), any(Pageable.class)
        );
    }

    @Test
    void rejectsEqualOrReversedInstantRanges() {
        assertThatThrownBy(() -> service.filterSystemMetrics(
                null, COLLECTED_AT, COLLECTED_AT, 0, 10, "collectedAt"
        )).isInstanceOf(InvalidSystemMetricException.class).hasMessage("start must be before end");
        assertThatThrownBy(() -> service.filterSystemMetrics(
                null, COLLECTED_AT.plusSeconds(1), COLLECTED_AT, 0, 10, "collectedAt"
        )).isInstanceOf(InvalidSystemMetricException.class).hasMessage("start must be before end");
        verifyNoInteractions(repository);
    }

    private CreateSystemMetricRequest validRequest() {
        return new CreateSystemMetricRequest(
                "lyftix-server", "local", 23.5, 400L, 1000L, 500L, 2000L, 1.25,
                COLLECTED_AT
        );
    }

    private SystemMetric savedMetric() {
        SystemMetric metric = mock(SystemMetric.class);
        when(metric.getId()).thenReturn(42L);
        when(metric.getHostname()).thenReturn("lyftix-server");
        when(metric.getSource()).thenReturn("local");
        when(metric.getCpuPercent()).thenReturn(23.5);
        when(metric.getMemoryUsedBytes()).thenReturn(400L);
        when(metric.getMemoryTotalBytes()).thenReturn(1000L);
        when(metric.getDiskUsedBytes()).thenReturn(500L);
        when(metric.getDiskTotalBytes()).thenReturn(2000L);
        when(metric.getLoadAverage1m()).thenReturn(1.25);
        when(metric.getCollectedAt()).thenReturn(COLLECTED_AT);
        when(metric.getCreatedAt()).thenReturn(COLLECTED_AT.plusSeconds(1));
        return metric;
    }
}
