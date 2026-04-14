-- Agent sessions
CREATE TABLE agent_sessions (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    title       VARCHAR(255),
    user_id     VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user_id ON agent_sessions (user_id);
CREATE INDEX idx_sessions_updated_at ON agent_sessions (updated_at DESC);

-- Conversation messages
CREATE TABLE conversation_messages (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id  VARCHAR(36)  NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    tool_name   VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id)
        REFERENCES agent_sessions (id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_session_id ON conversation_messages (session_id);
CREATE INDEX idx_messages_created_at ON conversation_messages (created_at);
