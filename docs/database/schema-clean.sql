-- AI Gateway Database Initialization Script
-- Clean version without Chinese characters

CREATE DATABASE IF NOT EXISTS ai_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_gateway;

-- API Key Table
CREATE TABLE IF NOT EXISTS `api_key` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `key_value` VARCHAR(128) NOT NULL,
  `key_name` VARCHAR(100) NOT NULL,
  `user_id` BIGINT NOT NULL,
  `tenant_id` VARCHAR(50),
  `app_id` VARCHAR(50),
  `status` TINYINT NOT NULL DEFAULT 1,
  `expire_time` DATETIME,
  `allowed_models` TEXT,
  `total_quota` BIGINT DEFAULT 0,
  `used_quota` BIGINT DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_value` (`key_value`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Model Table
CREATE TABLE IF NOT EXISTS `model` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `model_name` VARCHAR(100) NOT NULL,
  `model_version` VARCHAR(50),
  `provider` VARCHAR(50) NOT NULL,
  `description` TEXT,
  `status` TINYINT NOT NULL DEFAULT 1,
  `input_price` DECIMAL(10, 6),
  `output_price` DECIMAL(10, 6),
  `max_tokens` INT,
  `support_stream` TINYINT DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_model_name` (`model_name`),
  KEY `idx_provider` (`provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- API Key Application Table
CREATE TABLE IF NOT EXISTS `key_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `tenant_id` VARCHAR(50),
  `app_id` VARCHAR(50),
  `key_name` VARCHAR(100) NOT NULL,
  `allowed_models` TEXT,
  `reason` TEXT,
  `approval_status` TINYINT NOT NULL DEFAULT 0,
  `approver_id` BIGINT,
  `approval_comment` TEXT,
  `approval_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_approval_status` (`approval_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Provider Table
CREATE TABLE IF NOT EXISTS `provider` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(50) NOT NULL,
  `base_url` VARCHAR(255) NOT NULL,
  `api_key` VARCHAR(255) NOT NULL,
  `description` TEXT,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert sample data
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`) VALUES
('gpt-4', '1.0', 'openai', 'GPT-4 Model', 1, 0.03, 0.06, 8192, 1),
('gpt-3.5-turbo', '1.0', 'openai', 'GPT-3.5 Turbo Model', 1, 0.0015, 0.002, 4096, 1),
('claude-3-opus', '1.0', 'anthropic', 'Claude 3 Opus Model', 1, 0.015, 0.075, 200000, 1),
('claude-3-sonnet', '1.0', 'anthropic', 'Claude 3 Sonnet Model', 1, 0.003, 0.015, 200000, 1);

-- Insert Provider sample data
INSERT INTO `provider` (`name`, `code`, `base_url`, `api_key`, `description`, `status`) VALUES
('OpenAI', 'openai', 'https://api.openai.com/v1', 'sk-your-openai-api-key', 'OpenAI Official API Service', 1),
('DeepSeek', 'deepseek', 'https://api.deepseek.com/v1', 'sk-your-deepseek-api-key', 'DeepSeek AI Service', 1),
('Azure OpenAI', 'azure-openai', 'https://your-resource.openai.azure.com/openai/deployments/your-deployment', 'your-azure-api-key', 'Azure OpenAI Service', 0);
