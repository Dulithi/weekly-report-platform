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

        'USER_ROLE_CHANGED',
        'USER_ACTIVATED',
        'USER_DEACTIVATED'
    )
);