-- ============================================
-- 놀이터 (NoriTer) 데이터베이스 초기화 스크립트
-- MySQL 8.0
-- ============================================

CREATE TABLE IF NOT EXISTS project (
    id                  VARCHAR(20)   PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,
    requirement         TEXT          NOT NULL,
    genre               VARCHAR(20),
    status              VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    current_stage       VARCHAR(20),
    progress            INT           NOT NULL DEFAULT 0,
    debug_attempts      INT           NOT NULL DEFAULT 0,
    max_debug_attempts  INT           NOT NULL DEFAULT 3,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at        DATETIME(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    demo                TINYINT       NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stage (
    id              VARCHAR(20)   PRIMARY KEY,
    project_id      VARCHAR(20)   NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    agent_role      VARCHAR(30)   NOT NULL,
    stage_order     INT           NOT NULL,
    started_at      DATETIME(6),
    completed_at    DATETIME(6),
    artifact_id     VARCHAR(20),
    retry_count     INT           NOT NULL DEFAULT 0,
    error_message   TEXT,
    CONSTRAINT fk_stage_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT uq_stage_project_order UNIQUE (project_id, stage_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS artifact (
    id              VARCHAR(20)   PRIMARY KEY,
    project_id      VARCHAR(20)   NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    agent_role      VARCHAR(20)   NOT NULL,
    version         INT           NOT NULL DEFAULT 1,
    file_path       VARCHAR(500)  NOT NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_artifact_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS log_entry (
    id              VARCHAR(20)   PRIMARY KEY,
    project_id      VARCHAR(20)   NOT NULL,
    level           VARCHAR(10)   NOT NULL,
    agent_role      VARCHAR(20),
    stage           VARCHAR(20),
    message         TEXT          NOT NULL,
    error_code      VARCHAR(20),
    stack_trace     TEXT,
    resolved        TINYINT(1),
    resolved_by     VARCHAR(20),
    timestamp       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_log_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_message (
    id              VARCHAR(20)   PRIMARY KEY,
    project_id      VARCHAR(20)   NOT NULL,
    from_agent      VARCHAR(20)   NOT NULL,
    to_agent        VARCHAR(20)   NOT NULL,
    type            VARCHAR(20)   NOT NULL,
    content         TEXT          NOT NULL,
    artifact_ref    VARCHAR(20),
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_message_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- audit_log: FK 없음 — 프로젝트 삭제 후에도 90일 보존 (NFR-03)
CREATE TABLE IF NOT EXISTS audit_log (
    id              VARCHAR(20)   PRIMARY KEY,
    event_type      VARCHAR(30)   NOT NULL,
    project_id      VARCHAR(20),
    description     VARCHAR(500)  NOT NULL,
    detail          JSON,
    timestamp       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS setting (
    id              VARCHAR(20)   PRIMARY KEY,
    `key`           VARCHAR(100)  NOT NULL UNIQUE,
    value           TEXT          NOT NULL,
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS token_usage (
    id              VARCHAR(20)   PRIMARY KEY,
    project_id      VARCHAR(20)   NOT NULL,
    agent_role      VARCHAR(20)   NOT NULL,
    stage           VARCHAR(20)   NOT NULL,
    input_tokens    INT           NOT NULL DEFAULT 0,
    output_tokens   INT           NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_token_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 인덱스
CREATE INDEX idx_project_status_created ON project (status, created_at DESC);
CREATE INDEX idx_stage_project_order ON stage (project_id, stage_order);
CREATE INDEX idx_artifact_project ON artifact (project_id, created_at);
CREATE INDEX idx_artifact_project_type ON artifact (project_id, type, version DESC);
CREATE INDEX idx_log_project_time ON log_entry (project_id, timestamp DESC);
CREATE INDEX idx_log_project_level ON log_entry (project_id, level);
CREATE INDEX idx_log_project_agent ON log_entry (project_id, agent_role);
CREATE INDEX idx_message_project_time ON agent_message (project_id, created_at);
CREATE INDEX idx_message_project_agent ON agent_message (project_id, from_agent);
CREATE INDEX idx_audit_timestamp ON audit_log (timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_log (event_type, timestamp DESC);
CREATE INDEX idx_audit_project ON audit_log (project_id, timestamp DESC);
CREATE INDEX idx_token_project_agent ON token_usage (project_id, agent_role);
