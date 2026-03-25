-- AI Gateway 数据库初始化脚本

CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_gateway;

-- API Key 表
CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `key_value` VARCHAR(128) NOT NULL COMMENT 'API Key 值',
  `key_name` VARCHAR(100) NOT NULL COMMENT 'Key 名称',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `tenant_id` VARCHAR(50) COMMENT '租户 ID',
  `app_id` VARCHAR(50) COMMENT '应用 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `expire_time` DATETIME COMMENT '过期时间',
  `allowed_models` TEXT COMMENT '允许的模型列表',
  `total_quota` BIGINT DEFAULT 0 COMMENT '总配额',
  `used_quota` BIGINT DEFAULT 0 COMMENT '已用配额',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_value` (`key_value`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Key 表';

-- 模型表
CREATE TABLE IF NOT EXISTS `model` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_version` VARCHAR(50) COMMENT '模型版本',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商',
  `description` TEXT COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-不可用, 1-可用',
  `input_price` DECIMAL(10, 6) COMMENT '输入价格(元/1K tokens)',
  `output_price` DECIMAL(10, 6) COMMENT '输出价格(元/1K tokens)',
  `max_tokens` INT COMMENT '最大 tokens',
  `support_stream` TINYINT DEFAULT 1 COMMENT '是否支持流式: 0-否, 1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`),
  KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型表';


-- API Key 申请表
CREATE TABLE IF NOT EXISTS `key_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '申请用户 ID',
  `tenant_id` VARCHAR(50) COMMENT '租户 ID',
  `app_id` VARCHAR(50) COMMENT '应用 ID',
  `key_name` VARCHAR(100) NOT NULL COMMENT 'Key 名称',
  `allowed_models` TEXT COMMENT '申请的模型列表',
  `reason` TEXT COMMENT '申请理由',
  `approval_status` TINYINT NOT NULL DEFAULT 0 COMMENT '审批状态: 0-待审批, 1-已通过, 2-已拒绝',
  `approver_id` BIGINT COMMENT '审批人 ID',
  `approval_comment` TEXT COMMENT '审批意见',
  `approval_time` DATETIME COMMENT '审批时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API Key 申请表';

-- Provider 表
CREATE TABLE IF NOT EXISTS `provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(100) NOT NULL COMMENT 'Provider 名称',
  `code` VARCHAR(50) NOT NULL COMMENT 'Provider 唯一标识',
  `base_url` VARCHAR(255) NOT NULL COMMENT 'Base URL',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'API Key',
  `description` TEXT COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Provider 表';

-- 插入示例数据
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`) VALUES
('gpt-4', '1.0', 'openai', 'GPT-4 模型', 1, 0.03, 0.06, 8192, 1),
('gpt-3.5-turbo', '1.0', 'openai', 'GPT-3.5 Turbo 模型', 1, 0.0015, 0.002, 4096, 1),
('claude-3-opus', '1.0', 'anthropic', 'Claude 3 Opus 模型', 1, 0.015, 0.075, 200000, 1),
('claude-3-sonnet', '1.0', 'anthropic', 'Claude 3 Sonnet 模型', 1, 0.003, 0.015, 200000, 1);

-- 插入 Provider 示例数据
INSERT INTO `provider` (`name`, `code`, `base_url`, `api_key`, `description`, `status`) VALUES
('OpenAI', 'openai', 'https://api.openai.com/v1', 'sk-your-openai-api-key', 'OpenAI 官方API服务', 1),
('DeepSeek', 'deepseek', 'https://api.deepseek.com/v1', 'sk-your-deepseek-api-key', 'DeepSeek AI 服务', 1),
('Azure OpenAI', 'azure-openai', 'https://your-resource.openai.azure.com/openai/deployments/your-deployment', 'your-azure-api-key', 'Azure OpenAI 服务', 0);
