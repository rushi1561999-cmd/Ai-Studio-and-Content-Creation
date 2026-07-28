CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(40) NOT NULL,
    `read` BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS subscription_plans (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    monthly_credits INT NOT NULL,
    price_cents INT NOT NULL,
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS generation_jobs (
    id VARCHAR(36) PRIMARY KEY,
    prompt_text TEXT NOT NULL,
    status VARCHAR(255) NOT NULL,
    result TEXT,
    workspace_id VARCHAR(36) NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    model_key VARCHAR(100),
    media_url TEXT,
    created_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS generated_contents (
    id VARCHAR(36) PRIMARY KEY,
    generation_job_id VARCHAR(36) NOT NULL,
    workspace_id VARCHAR(36) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    ai_model_id VARCHAR(36),
    media_url TEXT,
    created_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS prompts (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category_id VARCHAR(36) NOT NULL,
    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

CREATE TABLE IF NOT EXISTS workspace_members (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(40) NOT NULL,
    joined_at DATETIME(6),
    CONSTRAINT uk_workspace_member UNIQUE (workspace_id, user_id)
);

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(36) PRIMARY KEY,
    workspace_id VARCHAR(36) NOT NULL,
    amount_cents INT NOT NULL,
    currency VARCHAR(10) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    external_id VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    credits_granted INT,
    created_at DATETIME(6)
);
