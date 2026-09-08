CREATE TABLE system_metrics (
    id BIGSERIAL PRIMARY KEY,
    hostname VARCHAR(255) NOT NULL,
    source VARCHAR(100) NOT NULL,
    cpu_percent DOUBLE PRECISION NOT NULL,
    memory_used_bytes BIGINT NOT NULL,
    memory_total_bytes BIGINT NOT NULL,
    disk_used_bytes BIGINT NOT NULL,
    disk_total_bytes BIGINT NOT NULL,
    load_average_1m DOUBLE PRECISION,
    collected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_system_metrics_hostname_nonblank CHECK (BTRIM(hostname) <> ''),
    CONSTRAINT ck_system_metrics_source_nonblank CHECK (BTRIM(source) <> ''),
    CONSTRAINT ck_system_metrics_cpu_percent CHECK (cpu_percent BETWEEN 0 AND 100),
    CONSTRAINT ck_system_metrics_memory_used CHECK (memory_used_bytes >= 0),
    CONSTRAINT ck_system_metrics_memory_total CHECK (memory_total_bytes > 0),
    CONSTRAINT ck_system_metrics_memory_bounds CHECK (memory_used_bytes <= memory_total_bytes),
    CONSTRAINT ck_system_metrics_disk_used CHECK (disk_used_bytes >= 0),
    CONSTRAINT ck_system_metrics_disk_total CHECK (disk_total_bytes > 0),
    CONSTRAINT ck_system_metrics_disk_bounds CHECK (disk_used_bytes <= disk_total_bytes),
    CONSTRAINT ck_system_metrics_load_average CHECK (load_average_1m IS NULL OR load_average_1m >= 0)
);

CREATE INDEX idx_system_metrics_collected_at ON system_metrics (collected_at);
CREATE INDEX idx_system_metrics_hostname_collected_at
    ON system_metrics (hostname, collected_at);
