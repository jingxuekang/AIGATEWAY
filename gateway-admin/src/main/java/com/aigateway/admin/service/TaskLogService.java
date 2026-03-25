package com.aigateway.admin.service;

import com.aigateway.admin.entity.TaskLog;
import com.aigateway.admin.mapper.TaskLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 任务日志服务（表 task_log，由定时任务 / 运维脚本写入）
 */
@Service
public class TaskLogService extends ServiceImpl<TaskLogMapper, TaskLog> {

    /**
     * 分页查询，按开始时间倒序
     */
    public Page<TaskLog> pageQuery(int pageNum, int pageSize,
                                   String type, String status,
                                   LocalDateTime startTimeFrom, LocalDateTime startTimeTo) {
        Page<TaskLog> p = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TaskLog> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type)) {
            w.eq(TaskLog::getType, type.trim());
        }
        if (StringUtils.hasText(status)) {
            w.eq(TaskLog::getStatus, status.trim());
        }
        if (startTimeFrom != null) {
            w.ge(TaskLog::getStartTime, startTimeFrom);
        }
        if (startTimeTo != null) {
            w.le(TaskLog::getStartTime, startTimeTo);
        }
        w.orderByDesc(TaskLog::getId);
        return page(p, w);
    }

    /** 记录一条任务日志（供定时任务调用） */
    public void record(String type, String status, LocalDateTime startTime, LocalDateTime endTime, String message) {
        TaskLog log = new TaskLog();
        log.setType(type);
        log.setStatus(status);
        log.setStartTime(startTime);
        log.setEndTime(endTime);
        log.setMessage(message);
        save(log);
    }
}

