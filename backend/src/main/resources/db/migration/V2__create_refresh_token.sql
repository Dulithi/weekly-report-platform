CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES app_user(id),

    token_hash CHAR(64) NOT NULL,

    family_id UUID NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_refresh_token_hash
        UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user
    ON refresh_token(user_id);

CREATE INDEX idx_refresh_token_family
    ON refresh_token(family_id);

CREATE INDEX idx_refresh_token_expires_at
    ON refresh_token(expires_at);