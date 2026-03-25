-- 清除示例数据，插入真实对接的模型
DELETE FROM `model` WHERE model_name IN ('gpt-4', 'gpt-3.5-turbo', 'claude-3-opus', 'claude-3-sonnet');

-- DeepSeek 模型
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`)
VALUES
  ('deepseek-chat',     'V3',   'deepseek', 'DeepSeek Chat - 通用对话模型，性价比高',        1, 0.14, 0.28, 65536, 1),
  ('deepseek-coder',    'V2.5', 'deepseek', 'DeepSeek Coder - 代码专用模型',                 1, 0.14, 0.28, 65536, 1),
  ('deepseek-reasoner', 'R1',   'deepseek', 'DeepSeek R1 - 深度推理模型',                    1, 4.00, 16.00, 65536, 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `input_price` = VALUES(`input_price`),
  `output_price` = VALUES(`output_price`);

-- Azure OpenAI 模型
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`)
VALUES
  ('azure-gpt-4o-mini', '2024-07-18', 'azure', 'Azure OpenAI GPT-4o mini - 快速高效的多模态模型', 1, 0.165, 0.66, 128000, 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `input_price` = VALUES(`input_price`),
  `output_price` = VALUES(`output_price`);

-- Volcano (火山引擎 Doubao) 模型
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`)
VALUES
  ('doubao-pro-32k',              '250115', 'volcano', 'Doubao Pro 32K - 高性能通用模型',          1, 0.8,  2.0,  32768,  1),
  ('doubao-lite-32k',             '240828', 'volcano', 'Doubao Lite 32K - 轻量快速模型',           1, 0.3,  0.6,  32768,  1),
  ('doubao-seed-2-0-pro-260215',  'latest', 'volcano', 'Doubao Seed 2.0 Pro - 多模态旗舰模型',     1, 1.0,  3.0,  32768,  1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `input_price` = VALUES(`input_price`),
  `output_price` = VALUES(`output_price`);

-- 验证插入结果
SELECT id, model_name, provider, status FROM `model` ORDER BY provider, model_name;

-- 智谱（GLM）模型
INSERT INTO `model` (`model_name`, `model_version`, `provider`, `description`, `status`, `input_price`, `output_price`, `max_tokens`, `support_stream`)
VALUES
  ('glm-4.5-air', 'latest', 'glm', 'GLM 4.5 Air - 通用文本对话模型', 1, 0.0, 0.0, 65536, 1),
  ('glm-4.6v',    'latest', 'glm', 'GLM 4.6V - 视觉多模态模型',     1, 0.0, 0.0, 65536, 1)
ON DUPLICATE KEY UPDATE
  `description` = VALUES(`description`),
  `status` = VALUES(`status`),
  `input_price` = VALUES(`input_price`),
  `output_price` = VALUES(`output_price`);
