CREATE INDEX idx_generation_jobs_workspace_created
    ON generation_jobs (workspace_id, created_at);

CREATE INDEX idx_generated_contents_workspace_created
    ON generated_contents (workspace_id, created_at);

CREATE INDEX idx_prompts_workspace_created
    ON prompts (workspace_id, created_at);

CREATE INDEX idx_notifications_user_read_created
    ON notifications (user_id, is_read, created_at);

CREATE INDEX idx_workspace_members_user_workspace
    ON workspace_members (user_id, workspace_id);

CREATE INDEX idx_payments_workspace_created
    ON payments (workspace_id, created_at);

CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    routing_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    available_at DATETIME(6) NOT NULL,
    published_at DATETIME(6),
    last_error VARCHAR(500),
    created_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_outbox_pending
    ON outbox_events (published_at, available_at, created_at);
