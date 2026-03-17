package com.aigateway.admin.service;

import com.aigateway.admin.entity.UsageLogRecord;
import com.aigateway.admin.es.EsUsageLogService;
import com.aigateway.admin.mapper.UsageLogRecordMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Usage 日志服务
 * 写入 MySQL 后异步同步到 ES
 */
@Slf4j
@Service
public class UsageLogRecordService extends ServiceImpl<UsageLogRecordMapper, UsageLogRecord> {

    private final EsUsageLogService esUsageLogService;

    public UsageLogRecordService(@Lazy EsUsageLogService esUsageLogService) {
        this.esUsageLogService = esUsageLogService;
    }

    /**
     * 保存日志到 MySQL，并异步写入 ES
     */
    public void saveAndIndex(UsageLogRecord record) {
        // 1. 写入 MySQL
        save(record);
        // 2. 异步写入 ES（失败不影响主流程）
        esUsageLogService.indexLog(record);
    }
}
