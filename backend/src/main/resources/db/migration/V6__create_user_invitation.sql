CREATE TABLE user_invitation (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    role VARCHAR(30) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    invited_by UUID NOT NULL REFERENCES app_user(id),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT chk_user_invitation_role
        CHECK (role IN ('TEAM_MEMBER', 'MANAGER', 'ADMIN')),
    CONSTRAINT chk_user_invitation_terminal_state
        CHECK (accepted_at IS NULL OR revoked_at IS NULL)
);

CREATE UNIQUE INDEX uk_user_invitation_pending_email
    ON user_invitation (LOWER(email))
    WHERE accepted_at IS NULL AND revoked_at IS NULL;

CREATE INDEX idx_user_invitation_created_at
    ON user_invitation (created_at DESC);

ALTER TABLE activity_log
DROP CONSTRAINT chk_activity_type;

ALTER TABLE activity_log
ADD CONSTRAINT chk_activity_type
CHECK (
    activity_type IN (
        'REPORT_CREATED',
        'REPORT_SUBMITTED',
        'REPORT_RESUBMITTED',
        'REPORT_CHANGES_REQUESTED',
        'REPORT_APPROVED',

        'PROJECT_CREATED',
        'PROJECT_UPDATED',
        'PROJECT_ARCHIVED',
        'PROJECT_ACTIVATED',

        'PROJECT_MEMBER_ASSIGNED',
        'PROJECT_MEMBER_REMOVED',

        'MANAGER_ASSIGNED',
        'MANAGER_REMOVED',

        'USER_INVITED',
        'INVITATION_ACCEPTED',
        'USER_ROLE_CHANGED',
        'USER_ACTIVATED',
        'USER_DEACTIVATED'
    )
);
