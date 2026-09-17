ALTER TABLE system_metrics
    ADD COLUMN uptime_seconds BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_system_metrics_uptime CHECK (uptime_seconds >= 0);

ALTER TABLE system_metrics
    ALTER COLUMN uptime_seconds DROP DEFAULT;
