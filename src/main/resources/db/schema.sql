CREATE TABLE IF NOT EXISTS topic (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    keywords VARCHAR(500),
    audience VARCHAR(200),
    direction VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_topic_status (status),
    INDEX idx_topic_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS note_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    topic_id BIGINT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(500),
    image_prompt VARCHAR(1000),
    image_asset_ids VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_note_id VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_note_draft_topic_id (topic_id),
    INDEX idx_note_draft_status (status),
    INDEX idx_note_draft_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS ai_generation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT,
    model VARCHAR(100),
    prompt TEXT NOT NULL,
    response MEDIUMTEXT,
    success TINYINT NOT NULL,
    error_message TEXT,
    duration_ms INT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_ai_generation_log_biz (biz_type, biz_id),
    INDEX idx_ai_generation_log_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS publish_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_draft_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    request_payload MEDIUMTEXT,
    response_payload MEDIUMTEXT,
    error_message TEXT,
    started_at DATETIME,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_publish_task_note_draft_id (note_draft_id),
    INDEX idx_publish_task_status (status),
    INDEX idx_publish_task_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS mcp_tool_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    input_schema MEDIUMTEXT,
    read_only TINYINT NOT NULL DEFAULT 0,
    destructive TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_mcp_tool_snapshot_name (name),
    INDEX idx_mcp_tool_snapshot_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS xhs_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    login_status VARCHAR(30) NOT NULL,
    profile_json MEDIUMTEXT,
    last_checked_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS image_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    prompt TEXT,
    tags VARCHAR(500),
    usage_scene VARCHAR(200),
    aspect_ratio VARCHAR(50),
    source VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    local_path VARCHAR(1000) NOT NULL,
    original_url VARCHAR(1000),
    mcp_response MEDIUMTEXT,
    remark VARCHAR(1000),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_image_asset_source (source),
    INDEX idx_image_asset_status (status),
    INDEX idx_image_asset_created_at (created_at)
);

