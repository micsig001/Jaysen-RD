-- ============================================
-- 仪器类产品研发管理系统 - V1 初始化全量建表
-- ============================================
-- 命名约定：
--   1. 业务主表用业务名（无前缀）：project / milestone / sprint_task / lab_equipment / ...
--   2. 系统基础表（用户/部门/审计/幂等）用 sys_ 前缀：sys_user / sys_department / sys_audit_log / sys_idempotency_key
--   3. 任务管理模块（从 Task 项目合并）保留原表名 task / task_status_history / tasks_history_archive
--   4. 敏捷看板任务用 sprint_task（避免和 task 重名）
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 系统基础表
-- ============================================

-- 1.1 企微应用配置（多应用支持，Phase 1 可只配 1 条）
CREATE TABLE IF NOT EXISTS wework_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    corp_id VARCHAR(64) NOT NULL COMMENT '企业ID',
    agent_id VARCHAR(64) NOT NULL COMMENT '应用ID',
    secret VARCHAR(128) NOT NULL COMMENT '应用Secret（加密存储）',
    token VARCHAR(128) COMMENT '回调Token',
    encoding_aes_key VARCHAR(128) COMMENT '回调EncodingAESKey',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用：1-启用 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_corp_agent (corp_id, agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业微信应用配置';

-- 1.2 用户表（统一 sys_user，覆盖原 Task 项目的 users）
-- 字段命名与 Task 项目保持一致（user_id / name / department_id / is_manual_role），
-- 这样 WeWorkAuthService / WeWorkSyncService 包搬入时无需重写 setter/getter
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) UNIQUE NOT NULL COMMENT '企微UserID',
    name VARCHAR(64) NOT NULL COMMENT '显示名称',
    avatar_url VARCHAR(512) COMMENT '头像URL',
    mobile VARCHAR(20) COMMENT '手机号（加密存储）',
    email VARCHAR(128) COMMENT '邮箱',
    department_id BIGINT COMMENT '所属部门ID',
    position VARCHAR(128) COMMENT '职位',
    role ENUM('EMPLOYEE', 'MANAGER', 'ADMIN') DEFAULT 'EMPLOYEE' COMMENT '系统角色',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常 0-禁用',
    is_manual_role TINYINT DEFAULT 0 COMMENT '是否手动分配角色（企微同步不覆盖）',
    last_login_at DATETIME COMMENT '最后登录时间',
    last_sync_time DATETIME COMMENT '最后从企微同步时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_role (role),
    INDEX idx_status (status),
    INDEX idx_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.3 部门表
CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_id VARCHAR(64) UNIQUE NOT NULL COMMENT '企微部门ID',
    name VARCHAR(128) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID（0=根）',
    order_num INT DEFAULT 0 COMMENT '排序号',
    leader_user_id VARCHAR(64) COMMENT '部门负责人UserID（企微）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 1.4 同步日志（企微 → 本地）
CREATE TABLE IF NOT EXISTS sys_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(32) NOT NULL COMMENT '同步类型：USER/DEPARTMENT',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发方式：MANUAL/SCHEDULED/INCREMENTAL/FULL',
    status VARCHAR(32) NOT NULL COMMENT '执行状态：RUNNING/SUCCESS/FAILED/PARTIAL',
    total_count INT DEFAULT 0 COMMENT '总条数',
    success_count INT DEFAULT 0 COMMENT '成功条数',
    failed_count INT DEFAULT 0 COMMENT '失败条数',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME NOT NULL COMMENT '开始时间',
    finished_at DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '耗时（毫秒）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微同步日志';

-- 1.5 审计日志
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id VARCHAR(64) COMMENT '操作人UserID',
    operator_name VARCHAR(128) COMMENT '操作人姓名（冗余）',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型：CREATE/UPDATE/DELETE/...',
    resource_type VARCHAR(64) NOT NULL COMMENT '资源类型：TASK/PROJECT/ECN/...',
    resource_id VARCHAR(128) COMMENT '资源ID',
    description VARCHAR(512) COMMENT '操作描述',
    before_data JSON COMMENT '变更前数据快照',
    after_data JSON COMMENT '变更后数据快照',
    ip_address VARCHAR(64) COMMENT '操作IP',
    user_agent VARCHAR(512) COMMENT 'UA',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_operator_time (operator_id, operation_time),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志';

-- 1.6 幂等性 Key 表（数据库兜底）
CREATE TABLE IF NOT EXISTS sys_idempotency_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    idempotency_key VARCHAR(64) UNIQUE NOT NULL COMMENT '幂等Key（UUID v4）',
    operation_type VARCHAR(64) NOT NULL COMMENT '操作类型',
    request_hash VARCHAR(128) COMMENT '请求体哈希（防同 key 不同 body）',
    response_data JSON COMMENT '首次返回结果',
    status ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED') DEFAULT 'IN_PROGRESS' COMMENT '状态',
    operator_id VARCHAR(64) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等性Key';

-- ============================================
-- 2. 任务管理模块（从 Task 项目合并）
-- ============================================

-- 2.1 任务主表（保留 Task 原 schema）
CREATE TABLE IF NOT EXISTS task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(32) UNIQUE NOT NULL COMMENT '任务编号',
    title VARCHAR(256) NOT NULL COMMENT '任务标题',
    description TEXT COMMENT '任务描述',
    source_remark VARCHAR(512) COMMENT '来源备注',
    creator_id VARCHAR(64) NOT NULL COMMENT '创建人UserID',
    assignee_id VARCHAR(64) COMMENT '执行人UserID',
    priority TINYINT DEFAULT 3 COMMENT '优先级 1-最高 2-高 3-中 4-低',
    status VARCHAR(32) DEFAULT 'PENDING_ACCEPT' COMMENT '状态：PENDING_ACCEPT/IN_PROGRESS/PENDING_VERIFY/COMPLETED/REJECTED/WITHDRAWN',
    self_assigned TINYINT DEFAULT 0 COMMENT '是否自己发给自己',
    estimated_duration INT COMMENT '预估时长（小时）',
    actual_start_time DATETIME COMMENT '实际开始时间',
    actual_deadline DATETIME COMMENT '截止时间（接收时计算）',
    actual_end_time DATETIME COMMENT '实际完成时间',
    reject_reason TEXT COMMENT '驳回原因',
    version INT DEFAULT 0 COMMENT '乐观锁',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_assignee_status (assignee_id, status),
    INDEX idx_creator_status (creator_id, status),
    INDEX idx_status_created (status, created_at),
    INDEX idx_deadline_status (actual_deadline, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务主表';

-- 2.2 任务状态流转历史
CREATE TABLE IF NOT EXISTS task_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    from_status VARCHAR(32) COMMENT '原状态',
    to_status VARCHAR(32) NOT NULL COMMENT '新状态',
    operator_id VARCHAR(64) NOT NULL COMMENT '操作人',
    operator_name VARCHAR(128) COMMENT '操作人姓名',
    remark TEXT COMMENT '备注/驳回原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_created (task_id, created_at),
    INDEX idx_operator_time (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务状态历史';

-- 2.3 任务归档表（冷数据）
CREATE TABLE IF NOT EXISTS tasks_history_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    original_task_id BIGINT NOT NULL COMMENT '原任务ID',
    task_no VARCHAR(32) NOT NULL COMMENT '任务编号',
    title VARCHAR(256) NOT NULL,
    creator_id VARCHAR(64) NOT NULL,
    assignee_id VARCHAR(64),
    priority TINYINT,
    final_status VARCHAR(32) NOT NULL COMMENT '归档时状态',
    self_assigned TINYINT,
    description TEXT,
    original_created_at DATETIME NOT NULL COMMENT '原创建时间',
    original_updated_at DATETIME COMMENT '原更新时间',
    archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '归档时间',
    archive_batch VARCHAR(64) COMMENT '归档批次号',
    INDEX idx_creator_archived (creator_id, archived_at),
    INDEX idx_task_no (task_no),
    INDEX idx_original_created (original_created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务归档表';

-- ============================================
-- 3. 项目管理模块（硬件甘特图 + 敏捷看板）
-- ============================================

-- 3.1 项目主表
CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) UNIQUE NOT NULL COMMENT '项目编号',
    name VARCHAR(256) NOT NULL COMMENT '项目名称',
    type ENUM('HARDWARE', 'FIRMWARE', 'SOFTWARE', 'MIXED') NOT NULL COMMENT '项目类型',
    phase ENUM('EVT', 'DVT', 'PVT', 'MP') COMMENT '硬件阶段（仅 HARDWARE/MIXED 使用）',
    manager_userid VARCHAR(64) NOT NULL COMMENT '项目负责人UserID（企微）',
    start_date DATE COMMENT '计划开始日期',
    end_date DATE COMMENT '计划结束日期',
    actual_start_date DATE COMMENT '实际开始',
    actual_end_date DATE COMMENT '实际结束',
    status ENUM('PLANNING', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETED', 'CANCELLED') DEFAULT 'PLANNING',
    progress DECIMAL(5,2) DEFAULT 0 COMMENT '完成百分比',
    description TEXT,
    tags JSON COMMENT '标签',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_manager (manager_userid),
    INDEX idx_status (status),
    INDEX idx_phase (phase),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目主表';

-- 3.2 里程碑（硬件甘特图）
CREATE TABLE IF NOT EXISTS milestone (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    name VARCHAR(256) NOT NULL COMMENT '里程碑名称',
    phase ENUM('EVT', 'DVT', 'PVT', 'MP') NOT NULL COMMENT '阶段',
    planned_start DATE NOT NULL,
    planned_end DATE NOT NULL,
    actual_start DATE,
    actual_end DATE,
    progress DECIMAL(5,2) DEFAULT 0,
    dependencies JSON COMMENT '前置里程碑ID列表',
    owner_userid VARCHAR(64) COMMENT '责任人UserID',
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'DELAYED') DEFAULT 'NOT_STARTED',
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_phase (phase),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目里程碑';

-- 3.3 敏捷冲刺
CREATE TABLE IF NOT EXISTS sprint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    name VARCHAR(256) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    goal TEXT COMMENT '冲刺目标',
    status ENUM('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PLANNED',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏捷冲刺';

-- 3.4 敏捷看板任务（spec 里的 task 表改名）
CREATE TABLE IF NOT EXISTS sprint_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL COMMENT '所属项目',
    sprint_id BIGINT COMMENT '所属冲刺（可空：BACKLOG）',
    title VARCHAR(512) NOT NULL,
    description TEXT,
    type ENUM('FEATURE', 'BUG', 'OPTIMIZATION', 'TEST') DEFAULT 'FEATURE',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') DEFAULT 'MEDIUM',
    status ENUM('BACKLOG', 'TODO', 'IN_PROGRESS', 'REVIEW', 'DONE') DEFAULT 'BACKLOG',
    assignee_userid VARCHAR(64),
    reporter_userid VARCHAR(64) NOT NULL,
    estimated_hours DECIMAL(8,2),
    actual_hours DECIMAL(8,2),
    story_points INT,
    tags JSON,
    attachments JSON,
    due_date DATE,
    completed_at DATETIME,
    order_num INT DEFAULT 0 COMMENT '看板排序（同列内）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES sprint(id) ON DELETE SET NULL,
    INDEX idx_project (project_id),
    INDEX idx_sprint (sprint_id),
    INDEX idx_assignee (assignee_userid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏捷看板任务';

-- ============================================
-- 4. 实验室资源管理
-- ============================================

-- 4.1 设备台账
CREATE TABLE IF NOT EXISTS lab_equipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_code VARCHAR(64) UNIQUE NOT NULL COMMENT '资产编号',
    name VARCHAR(256) NOT NULL,
    model VARCHAR(128),
    manufacturer VARCHAR(256),
    category ENUM('SPECTRUM_ANALYZER', 'OSCILLOSCOPE', 'SIGNAL_GENERATOR', 'NETWORK_ANALYZER', 'POWER_METER', 'OTHER') NOT NULL,
    location VARCHAR(256) COMMENT '存放位置',
    purchase_date DATE,
    warranty_expiry DATE,
    calibration_due_date DATE COMMENT '下次校准日期',
    calibration_interval_months INT COMMENT '校准周期',
    status ENUM('AVAILABLE', 'IN_USE', 'MAINTENANCE', 'CALIBRATION_OVERDUE', 'SCRAPPED') DEFAULT 'AVAILABLE',
    specifications JSON COMMENT '技术参数（key-value）',
    manual_url VARCHAR(512),
    qr_code VARCHAR(512) COMMENT '二维码图片URL',
    notes TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_category (category),
    INDEX idx_status (status),
    INDEX idx_calibration (calibration_due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账';

-- 4.2 设备预约
CREATE TABLE IF NOT EXISTS equipment_reservation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    user_id VARCHAR(64) NOT NULL COMMENT '预约人UserID（企微）',
    project_id BIGINT COMMENT '关联项目',
    purpose VARCHAR(512),
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'IN_USE', 'COMPLETED', 'CANCELLED', 'NO_SHOW') DEFAULT 'PENDING',
    actual_start_time DATETIME,
    actual_end_time DATETIME,
    notes TEXT,
    approved_by VARCHAR(64) COMMENT '审批人',
    approved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (equipment_id) REFERENCES lab_equipment(id),
    INDEX idx_equipment (equipment_id),
    INDEX idx_user (user_id),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备预约';

-- 4.3 设备维护记录
CREATE TABLE IF NOT EXISTS equipment_maintenance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    equipment_id BIGINT NOT NULL,
    type ENUM('ROUTINE', 'REPAIR', 'CALIBRATION', 'UPGRADE') NOT NULL,
    description TEXT NOT NULL,
    technician VARCHAR(128),
    cost DECIMAL(10,2),
    start_date DATE NOT NULL,
    end_date DATE,
    next_maintenance_date DATE,
    attachments JSON,
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (equipment_id) REFERENCES lab_equipment(id),
    INDEX idx_equipment (equipment_id),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维护记录';

-- ============================================
-- 5. BOM 与工程变更（Phase 2 启用）
-- ============================================

-- 5.1 BOM 表头
CREATE TABLE IF NOT EXISTS bom_header (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bom_code VARCHAR(64) UNIQUE NOT NULL,
    product_name VARCHAR(256) NOT NULL,
    product_model VARCHAR(128),
    version VARCHAR(32) NOT NULL DEFAULT '1.0',
    parent_bom_id BIGINT DEFAULT 0 COMMENT '父BOM ID（0=顶层）',
    project_id BIGINT COMMENT '关联项目',
    status ENUM('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'OBSOLETE') DEFAULT 'DRAFT',
    effective_date DATE,
    expiry_date DATE,
    created_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64),
    approved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_product (product_model),
    INDEX idx_version (version),
    INDEX idx_status (status),
    INDEX idx_parent (parent_bom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 表头';

-- 5.2 BOM 明细
CREATE TABLE IF NOT EXISTS bom_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    bom_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    item_code VARCHAR(64) NOT NULL COMMENT '物料编码',
    item_name VARCHAR(256) NOT NULL,
    specification VARCHAR(512),
    quantity DECIMAL(10,4) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    supplier VARCHAR(256),
    unit_price DECIMAL(10,4),
    total_price DECIMAL(12,4),
    remark TEXT,
    sub_bom_id BIGINT COMMENT '子BOM ID（多阶BOM）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (bom_id) REFERENCES bom_header(id) ON DELETE CASCADE,
    UNIQUE KEY uk_bom_line (bom_id, line_no),
    INDEX idx_item_code (item_code),
    INDEX idx_sub_bom (sub_bom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM 明细';

-- 5.3 ECN 工程变更
CREATE TABLE IF NOT EXISTS ecn_change (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ecn_number VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(512) NOT NULL,
    change_type ENUM('DESIGN', 'MATERIAL', 'PROCESS', 'DOCUMENT') NOT NULL,
    urgency ENUM('NORMAL', 'URGENT', 'CRITICAL') DEFAULT 'NORMAL',
    reason TEXT NOT NULL,
    description TEXT NOT NULL,
    impact_analysis TEXT,
    affected_bom_ids JSON COMMENT '受影响的BOM ID列表',
    requester_userid VARCHAR(64) NOT NULL,
    project_id BIGINT COMMENT '关联项目',
    status ENUM('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'IMPLEMENTED', 'CANCELLED') DEFAULT 'DRAFT',
    priority INT DEFAULT 0,
    target_date DATE,
    completed_at DATETIME,
    process_instance_id VARCHAR(64) COMMENT 'Flowable 流程实例ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_requester (requester_userid),
    INDEX idx_status (status),
    INDEX idx_process (process_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ECN 工程变更';

-- 5.4 ECN 审批记录
CREATE TABLE IF NOT EXISTS ecn_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ecn_id BIGINT NOT NULL,
    approver_userid VARCHAR(64) NOT NULL,
    department VARCHAR(128),
    role VARCHAR(64),
    step_order INT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'SKIPPED') DEFAULT 'PENDING',
    comment TEXT,
    signature_url VARCHAR(512),
    signed_at DATETIME,
    task_id VARCHAR(64) COMMENT 'Flowable Task ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (ecn_id) REFERENCES ecn_change(id) ON DELETE CASCADE,
    INDEX idx_ecn (ecn_id),
    INDEX idx_approver (approver_userid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ECN 审批记录';

-- ============================================
-- 6. 合规审计（Phase 3 启用）
-- ============================================

-- 6.1 文档审批
CREATE TABLE IF NOT EXISTS document_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_number VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(512) NOT NULL,
    type ENUM('DRAWING', 'SPECIFICATION', 'TEST_REPORT', 'USER_MANUAL', 'OTHER') NOT NULL,
    version VARCHAR(32) NOT NULL,
    file_url VARCHAR(512) NOT NULL,
    file_hash VARCHAR(128) COMMENT 'SHA256 防篡改',
    project_id BIGINT,
    requester_userid VARCHAR(64) NOT NULL,
    status ENUM('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED') DEFAULT 'DRAFT',
    approval_required TINYINT DEFAULT 1,
    process_instance_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档审批';

-- 6.2 电子签名记录
CREATE TABLE IF NOT EXISTS electronic_signature (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型：ECN / DOC_APPROVAL',
    business_id BIGINT NOT NULL,
    signer_userid VARCHAR(64) NOT NULL,
    signer_name VARCHAR(128) NOT NULL,
    signature_image_url VARCHAR(512),
    signature_data TEXT COMMENT '签名原始数据（Base64）',
    ip_address VARCHAR(64),
    user_agent VARCHAR(512),
    timestamp_ms BIGINT NOT NULL COMMENT '签署时间戳（毫秒）',
    verification_hash VARCHAR(256) COMMENT 'SHA256 验证哈希',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_type, business_id),
    INDEX idx_signer (signer_userid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子签名记录';

-- 6.3 缺陷跟踪
CREATE TABLE IF NOT EXISTS defect_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    defect_number VARCHAR(64) UNIQUE NOT NULL,
    title VARCHAR(512) NOT NULL,
    severity ENUM('CRITICAL', 'MAJOR', 'MINOR', 'TRIVIAL') NOT NULL,
    priority ENUM('HIGH', 'MEDIUM', 'LOW') DEFAULT 'MEDIUM',
    status ENUM('NEW', 'ANALYZING', 'FIX_IN_PROGRESS', 'FIXED', 'VERIFIED', 'CLOSED', 'REOPENED') DEFAULT 'NEW',
    phase_found VARCHAR(64) COMMENT '发现阶段（EVT/DVT/...）',
    root_cause TEXT,
    corrective_action TEXT,
    preventive_action TEXT,
    reporter_userid VARCHAR(64) NOT NULL,
    assignee_userid VARCHAR(64),
    verifier_userid VARCHAR(64),
    project_id BIGINT,
    sprint_task_id BIGINT COMMENT '关联的看板任务',
    found_date DATE NOT NULL,
    resolved_date DATE,
    verified_date DATE,
    closed_date DATE,
    attachments JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_status (status),
    INDEX idx_severity (severity),
    INDEX idx_reporter (reporter_userid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷跟踪';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 7. 关键复合索引（V1 末尾追加，确保业务表查询性能）
-- ============================================

-- 7.1 项目管理
CREATE INDEX idx_project_manager_status ON project(manager_userid, status);
CREATE INDEX idx_milestone_project_phase ON milestone(project_id, phase);
CREATE INDEX idx_sprint_project_status ON sprint(project_id, status);
CREATE INDEX idx_sprint_task_project_status ON sprint_task(project_id, status);
CREATE INDEX idx_sprint_task_sprint_status ON sprint_task(sprint_id, status);
CREATE INDEX idx_sprint_task_assignee_status ON sprint_task(assignee_userid, status);

-- 7.2 实验室资源
CREATE INDEX idx_equipment_category_status ON lab_equipment(category, status);
CREATE INDEX idx_equipment_calibration ON lab_equipment(calibration_due_date, status);
CREATE INDEX idx_reservation_equipment_time ON equipment_reservation(equipment_id, start_time, end_time);
CREATE INDEX idx_reservation_user_time ON equipment_reservation(user_id, start_time);

-- 7.3 BOM & ECN
CREATE INDEX idx_bom_header_product_version ON bom_header(product_model, version);
CREATE INDEX idx_bom_header_project_status ON bom_header(project_id, status);
CREATE INDEX idx_bom_item_bom_line ON bom_item(bom_id, line_no);
CREATE INDEX idx_ecn_status_urgency ON ecn_change(status, urgency);
CREATE INDEX idx_ecn_requester_status ON ecn_change(requester_userid, status);
CREATE INDEX idx_ecn_approval_ecn_status ON ecn_approval(ecn_id, status);
CREATE INDEX idx_ecn_approval_approver_status ON ecn_approval(approver_userid, status);

-- 7.4 合规审计
CREATE INDEX idx_document_project_status ON document_approval(project_id, status);
CREATE INDEX idx_signature_business ON electronic_signature(business_type, business_id);
CREATE INDEX idx_defect_project_status ON defect_tracking(project_id, status);
CREATE INDEX idx_defect_severity_status ON defect_tracking(severity, status);
CREATE INDEX idx_defect_reporter_time ON defect_tracking(reporter_userid, found_date);

-- ============================================
-- V1 结束
-- 后续 Flyway 版本：
--   V2__flowable_indexes.sql       - Flowable 7 ACT_* 表的查询索引（等 Flowable 自动建表后追加）
--                                     Flowable 7.1.0 自己的 ACT_RE_*/ACT_RU_*/ACT_HI_*/ACT_GE_*/ACT_ID_*
--                                     表由 application.yml 的 flowable.database-schema-update=true
--                                     在 Spring 启动时自动建，我们不再预建，避免版本错位
--   V3__seed_data.sql              - 初始种子数据（系统角色字典、设备类别字典等）
-- ============================================
