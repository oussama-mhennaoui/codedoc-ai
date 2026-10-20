-- V6__create_quality_reports.sql
CREATE TABLE quality_reports (
    id BIGSERIAL PRIMARY KEY,
    source_file_id BIGINT NOT NULL,
    score_global INT NOT NULL,
    technical_debt_minutes INT DEFAULT 0,
    technical_debt_rating VARCHAR(20) DEFAULT 'faible',
    raw_analysis JSONB NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_quality_reports_source_file FOREIGN KEY (source_file_id) REFERENCES source_files (id) ON DELETE CASCADE,
    CONSTRAINT uq_quality_reports_source_file UNIQUE (source_file_id)
);

CREATE INDEX idx_quality_reports_source_file_id ON quality_reports (source_file_id);
