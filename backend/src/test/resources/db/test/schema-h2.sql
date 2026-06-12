-- ============================================
-- H2 集成测试 schema（MySQL 兼容模式：MODE=MySQL）
-- 简化版主表，覆盖 @SpringBootTest 所需最小集合
-- ============================================
-- H2 与 MySQL 差异：
--   * AUTO_INCREMENT 仍可用
--   * TINYINT / BOOLEAN 仍可用
--   * ENUM 不可用 → 用 VARCHAR + CHECK 替代
--   * TEXT → CLOB
--   * DATETIME → TIMESTAMP
--   * JSON → 用 CLOB 存 JSON 字符串（应用层序列化）
-- ============================================

-- 1.1 企微应用配置
CREATE TABLE IF NOT EXISTS wework_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corp_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    secret VARCHAR(128) NOT NULL,
    token VARCHAR(128),
    encoding_aes_key VARCHAR(128),
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.2 用户表（sys_user，对齐 Task 字段）
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512),
    mobile VARCHAR(20),
    email VARCHAR(128),
    department_id BIGINT,
    position VARCHAR(128),
    role VARCHAR(20) NOT NULL DEFAULT 'EMPLOYEE',
    status TINYINT NOT NULL DEFAULT 1,
    is_manual_role BOOLEAN DEFAULT FALSE,
    last_login_at TIMESTAMP,
    last_sync_time TIMESTAMP,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.3 部门表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    order_num INT DEFAULT 0,
    leader_user_id VARCHAR(64),
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.4 同步日志
CREATE TABLE IF NOT EXISTS sys_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_count INT DEFAULT 0,
    success_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    error_message CLOB,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    duration_ms BIGINT,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.5 审计日志（@AuditLog 写入此表）
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id VARCHAR(512),
    operation_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128),
    description VARCHAR(512),
    before_data CLOB,
    after_data CLOB,
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    operation_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 1.6 幂等性 Key
CREATE TABLE IF NOT EXISTS sys_idempotency_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    operation_type VARCHAR(64) NOT NULL,
    request_hash VARCHAR(128),
    response_data CLOB,
    status VARCHAR(32) DEFAULT 'IN_PROGRESS',
    operator_id VARCHAR(64),
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

-- 2.1 任务主表
CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(32) NOT NULL UNIQUE,
    title VARCHAR(256) NOT NULL,
    description CLOB,
    source_remark VARCHAR(512),
    creator_id VARCHAR(64) NOT NULL,
    assignee_id VARCHAR(64),
    priority TINYINT DEFAULT 3,
    status VARCHAR(32) DEFAULT 'PENDING_ACCEPT',
    self_assigned TINYINT DEFAULT 0,
    estimated_duration INT,
    actual_start_time TIMESTAMP,
    actual_deadline TIMESTAMP,
    actual_end_time TIMESTAMP,
    reject_reason CLOB,
    version INT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2.2 任务状态历史
CREATE TABLE IF NOT EXISTS task_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operator_id VARCHAR(64) NOT NULL,
    operator_name VARCHAR(128),
    remark CLOB,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2.3 任务归档表
CREATE TABLE IF NOT EXISTS tasks_history_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_task_id BIGINT NOT NULL,
    task_no VARCHAR(32) NOT NULL,
    title VARCHAR(256) NOT NULL,
    creator_id VARCHAR(64) NOT NULL,
    assignee_id VARCHAR(64),
    priority TINYINT,
    final_status VARCHAR(32) NOT NULL,
    self_assigned TINYINT,
    description CLOB,
    original_created_at TIMESTAMP NOT NULL,
    original_updated_at TIMESTAMP,
    archived_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    archive_batch VARCHAR(64)
);
