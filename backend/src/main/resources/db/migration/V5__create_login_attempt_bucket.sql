CREATE TABLE login_attempt_bucket (
    bucket_key VARCHAR(64) PRIMARY KEY CHECK (length(bucket_key) = 64),
    attempts INTEGER NOT NULL CHECK (attempts > 0),
    window_ends_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_login_attempt_bucket_expiry ON login_attempt_bucket(window_ends_at);
