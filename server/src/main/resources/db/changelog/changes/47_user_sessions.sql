--liquibase formatted sql

--changeset thesis-management:47-create-user-sessions-table
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_identifier VARCHAR(64) NOT NULL,
    verifier_hash BYTEA NOT NULL,
    user_id UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP,
    end_reason VARCHAR(32),
    rotation_required BOOLEAN NOT NULL DEFAULT FALSE,
    superseded_by UUID,
    superseded_until TIMESTAMP,
    rotated_from UUID,
    csrf_token VARCHAR(64) NOT NULL,
    created_ip VARCHAR(64),
    last_ip VARCHAR(64),
    user_agent VARCHAR(255),
    user_agent_signature VARCHAR(64)
);

CREATE UNIQUE INDEX idx_user_sessions_token_identifier ON user_sessions (token_identifier);
CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);
CREATE INDEX idx_user_sessions_ended_at ON user_sessions (ended_at);
CREATE INDEX idx_user_sessions_rotated_from ON user_sessions (rotated_from);
