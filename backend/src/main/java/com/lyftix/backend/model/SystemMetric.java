package com.lyftix.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "system_metrics")
public class SystemMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String hostname;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(nullable = false)
    private Double cpuPercent;

    @Column(nullable = false)
    private Long memoryUsedBytes;

    @Column(nullable = false)
    private Long memoryTotalBytes;

    @Column(nullable = false)
    private Long diskUsedBytes;

    @Column(nullable = false)
    private Long diskTotalBytes;

    @Column(name = "load_average_1m")
    private Double loadAverage1m;

    @Column(nullable = false)
    private Instant collectedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Double getCpuPercent() { return cpuPercent; }
    public void setCpuPercent(Double cpuPercent) { this.cpuPercent = cpuPercent; }
    public Long getMemoryUsedBytes() { return memoryUsedBytes; }
    public void setMemoryUsedBytes(Long memoryUsedBytes) { this.memoryUsedBytes = memoryUsedBytes; }
    public Long getMemoryTotalBytes() { return memoryTotalBytes; }
    public void setMemoryTotalBytes(Long memoryTotalBytes) { this.memoryTotalBytes = memoryTotalBytes; }
    public Long getDiskUsedBytes() { return diskUsedBytes; }
    public void setDiskUsedBytes(Long diskUsedBytes) { this.diskUsedBytes = diskUsedBytes; }
    public Long getDiskTotalBytes() { return diskTotalBytes; }
    public void setDiskTotalBytes(Long diskTotalBytes) { this.diskTotalBytes = diskTotalBytes; }
    public Double getLoadAverage1m() { return loadAverage1m; }
    public void setLoadAverage1m(Double loadAverage1m) { this.loadAverage1m = loadAverage1m; }
    public Instant getCollectedAt() { return collectedAt; }
    public void setCollectedAt(Instant collectedAt) { this.collectedAt = collectedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
