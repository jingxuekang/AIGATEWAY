package com.aigateway.admin.service;

import com.aigateway.admin.entity.TaskLog;
import com.aigateway.admin.mapper.TaskLogMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 任务日志服务
 */
@Service
public class TaskLogService extends ServiceImpl<TaskLogMapper, TaskLog> {
}

