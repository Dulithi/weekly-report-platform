CREATE TABLE assistant_request_bucket (
    account_key VARCHAR(36) PRIMARY KEY,
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    window_ends_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_assistant_request_bucket_expiry
    ON assistant_request_bucket(window_ends_at);
