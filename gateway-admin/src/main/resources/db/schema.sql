-- API Key 表
CREATE TABLE IF NOT EXISTS api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_value VARCHAR(255) NOT NULL UNIQUE,
    key_name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    tenant_id VARCHAR(255),
    app_id VARCHAR(255),
    status INT DEFAULT 1,
    expire_time DATETIME,
    allowed_models TEXT,
    total_quota BIGINT DEFAULT 0,
    used_quota BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_key_value (key_value)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模型表
CREATE TABLE IF NOT EXISTS model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(255),
    provider VARCHAR(255) NOT NULL,
    description TEXT,
    status INT DEFAULT 1,
    input_price DECIMAL(10, 6),
    output_price DECIMAL(10, 6),
    max_tokens INT,
    support_stream BOOLEAN DEFAULT TRUE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_provider (provider),
    INDEX idx_model_name (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Key 申请表
CREATE TABLE IF NOT EXISTS key_application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tenant_id VARCHAR(255),
    app_id VARCHAR(255),
    key_name VARCHAR(255) NOT NULL,
    allowed_models TEXT,
    reason TEXT,
    approval_status INT DEFAULT 0,
    approver_id BIGINT,
    approval_comment TEXT,
    approval_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_approval_status (approval_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 渠道表
CREATE TABLE IF NOT EXISTS channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(255) NOT NULL,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_provider (provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 管理后台用户表
CREATE TABLE IF NOT EXISTS admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    role VARCHAR(50) DEFAULT 'admin',
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 充值记录表
CREATE TABLE IF NOT EXISTS top_up_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(20) DEFAULT 'CNY',
    status VARCHAR(50) DEFAULT 'success',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兑换码表
CREATE TABLE IF NOT EXISTS redemption_code (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'unused',
    expire_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统设置表
CREATE TABLE IF NOT EXISTS system_setting (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 任务日志表
CREATE TABLE IF NOT EXISTS task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    message TEXT,
    deleted INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usage 日志表
CREATE TABLE IF NOT EXISTS usage_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    timestamp DATETIME,
    trace_id VARCHAR(64),
    request_id VARCHAR(128),
    tenant_id VARCHAR(255),
    app_id VARCHAR(255),
    user_id VARCHAR(255),
    model VARCHAR(255),
    provider VARCHAR(255),
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    cache_creation_tokens INT,
    cache_read_tokens INT,
    status VARCHAR(50),
    latency_ms BIGINT,
    ttft_ms BIGINT,
    tpot_ms BIGINT,
    error_code VARCHAR(64),
    error_message TEXT,
    INDEX idx_timestamp (timestamp),
    INDEX idx_model (model),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 模型订阅表
CREATE TABLE IF NOT EXISTS model_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    tenant_id VARCHAR(255),
    app_id VARCHAR(255),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_model (user_id, model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
