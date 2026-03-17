-- 补丁脚本：在已有 schema.sql 基础上补充缺失字段
-- 执行前请确认表已存在

-- usage_log 补充 error_code / error_message（若不存在）
ALTER TABLE usage_log
    ADD COLUMN IF NOT EXISTS error_code    VARCHAR(64)  COMMENT '错误码',
    ADD COLUMN IF NOT EXISTS error_message TEXT         COMMENT '错误信息';

-- usage_log 补充缺失索引
CREATE INDEX IF NOT EXISTS idx_tenant_id  ON usage_log(tenant_id);
CREATE INDEX IF NOT EXISTS idx_trace_id   ON usage_log(trace_id);
CREATE INDEX IF NOT EXISTS idx_provider   ON usage_log(provider);

-- key_application 补充 tenant_id / app_id（若表结构旧版缺少）
ALTER TABLE key_application
    ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(255) COMMENT '租户 ID',
    ADD COLUMN IF NOT EXISTS app_id    VARCHAR(255) COMMENT '应用 ID';

-- model_subscription 补充 approval_status（订阅审批状态）
ALTER TABLE model_subscription
    ADD COLUMN IF NOT EXISTS approval_status INT DEFAULT 0 COMMENT '0=待审批 1=已批准 2=已拒绝',
    ADD COLUMN IF NOT EXISTS approver_id     BIGINT        COMMENT '审批人 ID',
    ADD COLUMN IF NOT EXISTS approval_comment TEXT         COMMENT '审批意见',
    ADD COLUMN IF NOT EXISTS approval_time   DATETIME      COMMENT '审批时间';

-- 初始化默认管理员账号（密码: Admin@123，BCrypt 加密）
-- 首次部署时执行；密码请在生产环境部署后立即修改
INSERT IGNORE INTO admin_user(username, password, role, status)
VALUES (
    ''admin'',
    ''$2a$10$7QxvbH7KhXvLzFqK9mXzCOeQqK3vXkL2yWzXmN8pP5qR6sT4uV1W2'',
    ''admin'',
    1
);
