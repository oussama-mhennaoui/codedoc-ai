CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    language VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    archived BOOLEAN DEFAULT false,
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE source_files (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    language VARCHAR(50),
    size_bytes BIGINT,
    checksum VARCHAR(64),
    uploaded_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_source_files_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT uq_source_files_project_filename UNIQUE (project_id, filename)
);

CREATE TABLE generated_docs (
    id BIGSERIAL PRIMARY KEY,
    source_file_id BIGINT NOT NULL,
    model_used VARCHAR(100),
    prompt_version VARCHAR(50),
    raw_response JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT chk_generated_docs_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED')),
    CONSTRAINT fk_generated_docs_source_file FOREIGN KEY (source_file_id) REFERENCES source_files (id) ON DELETE CASCADE
);

CREATE TABLE doc_sections (
    id BIGSERIAL PRIMARY KEY,
    generated_doc_id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,
    order_index INT DEFAULT 0,
    type VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT chk_doc_sections_type CHECK (type IN ('SUMMARY', 'FUNCTION', 'CLASS', 'EXAMPLE', 'NOTE')),
    CONSTRAINT fk_doc_sections_generated_doc FOREIGN KEY (generated_doc_id) REFERENCES generated_docs (id) ON DELETE CASCADE
);

CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    doc_section_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_comments_doc_section FOREIGN KEY (doc_section_id) REFERENCES doc_sections (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE ai_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    endpoint VARCHAR(100),
    prompt_tokens INT,
    completion_tokens INT,
    latency_ms INT,
    status VARCHAR(20),
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_ai_logs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_projects_owner_id ON projects (owner_id);
CREATE INDEX idx_source_files_project_id ON source_files (project_id);
CREATE INDEX idx_generated_docs_source_file_id ON generated_docs (source_file_id);
CREATE INDEX idx_doc_sections_generated_doc_id ON doc_sections (generated_doc_id);
CREATE INDEX idx_comments_doc_section_id ON comments (doc_section_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);
CREATE INDEX idx_ai_logs_user_id ON ai_logs (user_id);
