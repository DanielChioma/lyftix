CREATE TABLE github_activity (
    id BIGSERIAL PRIMARY KEY,
    activity_type VARCHAR(50) NOT NULL,
    repository_name VARCHAR(255) NOT NULL,
    repository_owner VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_github_activity_external_id UNIQUE (external_id),
    CONSTRAINT ck_github_activity_type_not_blank CHECK (BTRIM(activity_type) <> ''),
    CONSTRAINT ck_github_activity_repository_name_not_blank CHECK (BTRIM(repository_name) <> ''),
    CONSTRAINT ck_github_activity_repository_owner_not_blank CHECK (BTRIM(repository_owner) <> ''),
    CONSTRAINT ck_github_activity_external_id_not_blank CHECK (BTRIM(external_id) <> ''),
    CONSTRAINT ck_github_activity_title_not_blank CHECK (BTRIM(title) <> '')
);

CREATE INDEX idx_github_activity_occurred_at
    ON github_activity (occurred_at DESC);

CREATE INDEX idx_github_activity_type_occurred_at
    ON github_activity (activity_type, occurred_at DESC);

CREATE INDEX idx_github_activity_repository_occurred_at
    ON github_activity (repository_owner, repository_name, occurred_at DESC);
