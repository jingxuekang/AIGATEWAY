-- 任务日志表 task_log 示例数据（运维 / 开发自测）
-- 实际应由定时任务在开始时插入 running，结束时更新为 success/failed 并写入 end_time、message

INSERT INTO task_log (`type`, `status`, `start_time`, `end_time`, `message`, `deleted`)
VALUES
  ('quota_reset', 'success', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR + INTERVAL 5 MINUTE, '配额日切完成', 0),
  ('log_cleanup', 'running', NOW() - INTERVAL 10 MINUTE, NULL, NULL, 0),
  ('model_sync', 'failed', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 30 SECOND, '上游超时', 0);

-- Java 侧也可调用 TaskLogService.record(type, status, startTime, endTime, message) 写入
