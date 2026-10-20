CREATE TABLE security_audits (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT REFERENCES projects(id),
    source_file_id BIGINT REFERENCES source_files(id),
    status VARCHAR(20) NOT NULL,
    security_score VARCHAR(2),
    summary TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    model_used VARCHAR(255),
    prompt_version VARCHAR(255),
    raw_response TEXT
);

CREATE TABLE vulnerabilities (
    id BIGSERIAL PRIMARY KEY,
    audit_id BIGINT NOT NULL REFERENCES security_audits(id) ON DELETE CASCADE,
    type VARCHAR(255),
    severity VARCHAR(20),
    file_name VARCHAR(255),
    line INTEGER,
    description TEXT,
    consequences TEXT,
    fix TEXT,
    cwe_id VARCHAR(50),
    corrected_code TEXT
);
