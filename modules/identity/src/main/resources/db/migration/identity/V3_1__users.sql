CREATE TABLE users (
    id             UUID         PRIMARY KEY,
    tenant_id      UUID         NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(255) NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_tenant ON users (tenant_id);

CREATE TABLE user_roles (
    user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(32) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
