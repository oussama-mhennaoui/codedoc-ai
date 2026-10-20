-- Module 3: Chatbot contextuel RAG
-- Tables pour les conversations et messages du chatbot

CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT fk_chat_conv_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_conv_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sources JSONB,
    created_at TIMESTAMPTZ DEFAULT now(),
    CONSTRAINT chk_chat_msg_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT fk_chat_msg_conv FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_conversations_project_id ON chat_conversations (project_id);
CREATE INDEX idx_chat_conversations_user_id ON chat_conversations (user_id);
CREATE INDEX idx_chat_messages_conversation_id ON chat_messages (conversation_id);
