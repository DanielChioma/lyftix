CREATE TABLE coding_sessions (
    id BIGSERIAL PRIMARY KEY,
    project_name VARCHAR(255) NOT NULL,
    language VARCHAR(100) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ended_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_coding_sessions_project_name_not_blank CHECK (BTRIM(project_name) <> ''),
    CONSTRAINT ck_coding_sessions_language_not_blank CHECK (BTRIM(language) <> ''),
    CONSTRAINT ck_coding_sessions_source_not_blank CHECK (BTRIM(source) <> ''),
    CONSTRAINT ck_coding_sessions_time_order CHECK (ended_at > started_at)
);

CREATE INDEX idx_coding_sessions_started_at
    ON coding_sessions (started_at DESC);

CREATE INDEX idx_coding_sessions_project_started_at
    ON coding_sessions (project_name, started_at DESC);

CREATE INDEX idx_coding_sessions_language_started_at
    ON coding_sessions (language, started_at DESC);
