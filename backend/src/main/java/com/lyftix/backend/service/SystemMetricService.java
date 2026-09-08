package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateSystemMetricRequest;
import com.lyftix.backend.dto.SystemMetricResponse;
import com.lyftix.backend.exception.InvalidSystemMetricException;
import com.lyftix.backend.model.SystemMetric;
import com.lyftix.backend.repository.SystemMetricRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class SystemMetricService {

    private final SystemMetricRepository repository;

    public SystemMetricService(SystemMetricRepository repository) {
        this.repository = repository;
    }

    public SystemMetricResponse createSystemMetric(CreateSystemMetricRequest request) {
        validateByteRelationships(request);
        SystemMetric metric = new SystemMetric();
        metric.setHostname(request.hostname());
        metric.setSource(request.source());
        metric.setCpuPercent(request.cpuPercent());
        metric.setMemoryUsedBytes(request.memoryUsedBytes());
        metric.setMemoryTotalBytes(request.memoryTotalBytes());
        metric.setDiskUsedBytes(request.diskUsedBytes());
        metric.setDiskTotalBytes(request.diskTotalBytes());
        metric.setLoadAverage1m(request.loadAverage1m());
        metric.setCollectedAt(request.collectedAt());
        return toResponse(repository.save(metric));
    }

    public List<SystemMetricResponse> getAllSystemMetrics() {
        return repository.findAll(Sort.by("collectedAt").descending()).stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<SystemMetricResponse> getSystemMetrics(int page, int size, String sortBy) {
        return repository.findAll(pageable(page, size, sortBy)).map(this::toResponse);
    }

    public Page<SystemMetricResponse> filterSystemMetrics(
            String hostname, Instant start, Instant end, int page, int size, String sortBy
    ) {
        validateFilters(hostname, start, end);
        return repository.findByFilters(hostname, start, end, pageable(page, size, sortBy))
                .map(this::toResponse);
    }

    private void validateByteRelationships(CreateSystemMetricRequest request) {
        if (request.memoryUsedBytes() > request.memoryTotalBytes()) {
            throw new InvalidSystemMetricException("memoryUsedBytes must not exceed memoryTotalBytes");
        }
        if (request.diskUsedBytes() > request.diskTotalBytes()) {
            throw new InvalidSystemMetricException("diskUsedBytes must not exceed diskTotalBytes");
        }
    }

    private void validateFilters(String hostname, Instant start, Instant end) {
        if (hostname != null && hostname.isBlank()) {
            throw new InvalidSystemMetricException("hostname must not be blank");
        }
        if ((start == null) != (end == null)) {
            throw new InvalidSystemMetricException("start and end must be provided together");
        }
        if (start != null && !start.isBefore(end)) {
            throw new InvalidSystemMetricException("start must be before end");
        }
    }

    private Pageable pageable(int page, int size, String sortBy) {
        return PageRequest.of(page, size, Sort.by(sortBy).descending());
    }

    private SystemMetricResponse toResponse(SystemMetric metric) {
        return new SystemMetricResponse(
                metric.getId(), metric.getHostname(), metric.getSource(), metric.getCpuPercent(),
                metric.getMemoryUsedBytes(), metric.getMemoryTotalBytes(), metric.getDiskUsedBytes(),
                metric.getDiskTotalBytes(), metric.getLoadAverage1m(), metric.getCollectedAt(),
                metric.getCreatedAt()
        );
    }
}
