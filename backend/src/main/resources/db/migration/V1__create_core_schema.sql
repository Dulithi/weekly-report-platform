CREATE TABLE app_user (
    id UUID PRIMARY KEY,

    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,

    role VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CONSTRAINT chk_app_user_role CHECK (role IN ('TEAM_MEMBER', 'MANAGER', 'ADMIN'))
);


CREATE UNIQUE INDEX uk_app_user_email_lower ON app_user (LOWER(email));

CREATE TABLE manager_team_member (
    team_member_id UUID PRIMARY KEY
        REFERENCES app_user(id),

    manager_id UUID NOT NULL
        REFERENCES app_user(id),

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_manager_not_self
        CHECK (manager_id <> team_member_id)
);

CREATE TABLE project (
    id UUID PRIMARY KEY,

    name VARCHAR(150) NOT NULL,
    description TEXT,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NOT NULL REFERENCES app_user(id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_project_status CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_project_name_lower 
    ON project (LOWER(name)) 
    WHERE status = 'ACTIVE';

CREATE TABLE project_member (
    project_id UUID NOT NULL REFERENCES project(id),

    user_id UUID NOT NULL REFERENCES app_user(id),

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE weekly_report (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL
        REFERENCES app_user(id),

    week_start DATE NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    current_version_id UUID,

    submitted_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    entity_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_weekly_report_status
        CHECK (
            status IN (
                'DRAFT',
                'SUBMITTED',
                'NEEDS_CORRECTION',
                'APPROVED'
            )
        ),

    CONSTRAINT chk_weekly_report_monday
        CHECK (EXTRACT(ISODOW FROM week_start) = 1),

    CONSTRAINT uk_weekly_report_user_week
        UNIQUE (user_id, week_start)
);

CREATE TABLE report_version (
    id UUID PRIMARY KEY,

    report_id UUID NOT NULL
        REFERENCES weekly_report(id),

    version_number INTEGER NOT NULL,

    notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMPTZ,

    CONSTRAINT chk_report_version_number
        CHECK (version_number > 0),

    CONSTRAINT uk_report_version_number
        UNIQUE (report_id, version_number),

    CONSTRAINT uk_report_version_id_report
        UNIQUE (id, report_id)
);

ALTER TABLE weekly_report
ADD CONSTRAINT fk_weekly_report_current_version
FOREIGN KEY (current_version_id, id)
REFERENCES report_version(id, report_id);

CREATE TABLE completed_task (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL 
        REFERENCES report_version(id)
        ON DELETE CASCADE,

    project_id UUID REFERENCES project(id),

    task_name VARCHAR(255) NOT NULL,
    description TEXT,

    priority VARCHAR(20) NOT NULL,

    planned_percentage INTEGER NOT NULL,
    actual_percentage INTEGER NOT NULL,

    status VARCHAR(30) NOT NULL,

    planned_minutes INTEGER,
    spent_minutes INTEGER,

    deliverable TEXT,

    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_completed_task_priority
        CHECK (
            priority IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_completed_task_status
        CHECK (
            status IN (
                'NOT_STARTED',
                'IN_PROGRESS',
                'COMPLETED',
                'BLOCKED'
            )
        ),

    CONSTRAINT chk_completed_task_planned_percentage CHECK (planned_percentage BETWEEN 0 AND 100),

    CONSTRAINT chk_completed_task_actual_percentage CHECK (actual_percentage BETWEEN 0 AND 100),

    CONSTRAINT chk_completed_task_planned_minutes
        CHECK (
            planned_minutes IS NULL
            OR planned_minutes >= 0
        ),

    CONSTRAINT chk_completed_task_spent_minutes
        CHECK (
            spent_minutes IS NULL
            OR spent_minutes >= 0
        )
);

CREATE TABLE planned_task (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL REFERENCES report_version(id) ON DELETE CASCADE,

    project_id UUID REFERENCES project(id),

    task_name VARCHAR(255) NOT NULL,
    description TEXT,

    priority VARCHAR(20) NOT NULL,

    estimated_minutes INTEGER,

    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT chk_planned_task_priority
        CHECK (
            priority IN (
                'LOW',
                'MEDIUM',
                'HIGH',
                'CRITICAL'
            )
        ),

    CONSTRAINT chk_planned_task_estimated_minutes
        CHECK (
            estimated_minutes IS NULL
            OR estimated_minutes >= 0
        )
);

CREATE TABLE blocker (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL
        REFERENCES report_version(id)
        ON DELETE CASCADE,

    description TEXT NOT NULL,

    is_key_blocker BOOLEAN NOT NULL DEFAULT FALSE,

    resolved BOOLEAN NOT NULL DEFAULT FALSE,

    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_blocker_key_per_version
    ON blocker (report_version_id)
    WHERE is_key_blocker = TRUE;

CREATE TABLE achievement (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL
        REFERENCES report_version(id)
        ON DELETE CASCADE,

    description TEXT NOT NULL,

    is_key_achievement BOOLEAN NOT NULL DEFAULT FALSE,

    sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_achievement_key_per_version
    ON achievement (report_version_id)
    WHERE is_key_achievement = TRUE;


CREATE TABLE time_entry (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL
        REFERENCES report_version(id)
        ON DELETE CASCADE,

    task_type VARCHAR(30) NOT NULL,

    minutes INTEGER NOT NULL,

    CONSTRAINT chk_time_entry_task_type
        CHECK (
            task_type IN (
                'DEVELOPMENT',
                'TESTING',
                'MEETINGS',
                'DOCUMENTATION',
                'RESEARCH',
                'DESIGN',
                'OTHER'
            )
        ),

    CONSTRAINT chk_time_entry_minutes CHECK (minutes >= 0),

    CONSTRAINT uk_time_entry_type_per_version UNIQUE (report_version_id, task_type)
);

CREATE TABLE review (
    id UUID PRIMARY KEY,

    report_version_id UUID NOT NULL
        REFERENCES report_version(id),

    reviewer_id UUID NOT NULL
        REFERENCES app_user(id),

    action VARCHAR(30) NOT NULL,

    comment TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_review_action
        CHECK (
            action IN (
                'APPROVED',
                'CHANGES_REQUESTED'
            )
        ),

    CONSTRAINT chk_changes_requested_comment
        CHECK (
            action <> 'CHANGES_REQUESTED'
            OR (
                comment IS NOT NULL
                AND LENGTH(TRIM(comment)) > 0
            )
        )
);

CREATE TABLE report_status_history (
    id UUID PRIMARY KEY,

    report_id UUID NOT NULL
        REFERENCES weekly_report(id),

    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,

    changed_by UUID NOT NULL
        REFERENCES app_user(id),

    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_status_history_from
        CHECK (
            from_status IS NULL
            OR from_status IN (
                'DRAFT',
                'SUBMITTED',
                'NEEDS_CORRECTION',
                'APPROVED'
            )
        ),

    CONSTRAINT chk_status_history_to
        CHECK (
            to_status IN (
                'DRAFT',
                'SUBMITTED',
                'NEEDS_CORRECTION',
                'APPROVED'
            )
        )
);

CREATE TABLE activity_log (
    id UUID PRIMARY KEY,

    actor_user_id UUID REFERENCES app_user(id),

    activity_type VARCHAR(50) NOT NULL,

    entity_id UUID,

    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_activity_type
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

                'PROJECT_MEMBER_ASSIGNED',
                'PROJECT_MEMBER_REMOVED',

                'MANAGER_ASSIGNED',

                'USER_ROLE_CHANGED',
                'USER_ACTIVATED',
                'USER_DEACTIVATED'
            )
        )
);

CREATE INDEX idx_project_member_user
    ON project_member(user_id);


CREATE INDEX idx_weekly_report_user
    ON weekly_report(user_id);

CREATE INDEX idx_weekly_report_week_start
    ON weekly_report(week_start);

CREATE INDEX idx_weekly_report_status
    ON weekly_report(status);

CREATE INDEX idx_weekly_report_status_week
    ON weekly_report(status, week_start);


CREATE INDEX idx_completed_task_report_version
    ON completed_task(report_version_id);

CREATE INDEX idx_completed_task_project
    ON completed_task(project_id);


CREATE INDEX idx_planned_task_report_version
    ON planned_task(report_version_id);

CREATE INDEX idx_planned_task_project
    ON planned_task(project_id);


CREATE INDEX idx_blocker_report_version
    ON blocker(report_version_id);


CREATE INDEX idx_achievement_report_version
    ON achievement(report_version_id);


CREATE INDEX idx_review_report_version
    ON review(report_version_id);

CREATE INDEX idx_review_created_at
    ON review(created_at DESC);


CREATE INDEX idx_status_history_report
    ON report_status_history(report_id);

CREATE INDEX idx_status_history_changed_at
    ON report_status_history(changed_at DESC);

CREATE INDEX idx_activity_log_created_at
    ON activity_log(created_at DESC);

CREATE INDEX idx_activity_log_entity_id
    ON activity_log(entity_id);

CREATE INDEX idx_activity_log_type_created_at
    ON activity_log(activity_type, created_at DESC);

CREATE INDEX idx_manager_team_member_manager
    ON manager_team_member(manager_id);