-- V9__create_test_generations.sql
CREATE TABLE test_generations (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    source_file_id  BIGINT,
    language        VARCHAR(50)  NOT NULL,
    test_framework  VARCHAR(50)  NOT NULL,
    source_code_snippet TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    result          JSONB,
    error_message   TEXT,
    model_used      VARCHAR(100),
    prompt_version  VARCHAR(50),
    created_at      TIMESTAMPTZ  DEFAULT now(),
    CONSTRAINT chk_test_gen_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT fk_test_gen_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_test_gen_source_file FOREIGN KEY (source_file_id) REFERENCES source_files (id) ON DELETE SET NULL
);

CREATE INDEX idx_test_gen_user_id ON test_generations (user_id);
CREATE INDEX idx_test_gen_source_file_id ON test_generations (source_file_id);
