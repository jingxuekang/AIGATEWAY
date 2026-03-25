-- =============================================================================
-- 数据清理示例（执行前请自行备份库；在测试环境先验证）
-- 数据库：ai_gateway（与 application.yml 一致）
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 图一：删除用户 jimmy 名下全部 API Key（推荐逻辑删除，与 MyBatis 逻辑删除一致）
-- -----------------------------------------------------------------------------
UPDATE api_key
SET deleted = 1, update_time = NOW()
WHERE deleted = 0
  AND user_id = (SELECT id FROM `user` WHERE username = 'jimmy' AND deleted = 0 LIMIT 1);

-- 若需物理删除（慎用，且需先处理外键/历史日志依赖）
-- DELETE FROM api_key WHERE user_id = (SELECT id FROM `user` WHERE username = 'jimmy' AND deleted = 0 LIMIT 1);

-- -----------------------------------------------------------------------------
-- 图二：去掉「历史调用里出现、但已不再使用的模型」排行数据
-- 说明：排行来自 usage_log；若只想删掉某几条模型名的历史记录，可执行：
-- -----------------------------------------------------------------------------
-- DELETE FROM usage_log WHERE model IN ('gpt-3.5-turbo', 'doubao-pro-32k');

-- 若希望删除「当前 model 表里不存在」的所有历史（更激进）：
-- DELETE u FROM usage_log u
-- LEFT JOIN model m ON m.model_name = u.model AND m.deleted = 0
-- WHERE m.id IS NULL;

-- -----------------------------------------------------------------------------
-- 图三说明（无需删库即可理解）
-- provider 列来源：网关写日志时用 DynamicChannelProvider.getProviderName()，
-- 形如 channel:{渠道ID}:{厂商代码}，与静态 Provider Bean 的 deepseek/openai 等并存，
-- 因此会出现多行。代码已改为按 SUBSTRING_INDEX(provider,':',-1) 聚合为「厂商维度」。
-- -----------------------------------------------------------------------------
