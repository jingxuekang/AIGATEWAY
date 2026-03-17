package com.aigateway.admin.controller;

import com.aigateway.admin.entity.UsageLogRecord;
import com.aigateway.admin.service.UsageLogRecordService;
import com.aigateway.common.dto.UsageLogDTO;
import com.aigateway.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Usage Logs", description = "调用日志采集与查询")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final UsageLogRecordService usageLogRecordService;

    @Operation(summary = "接收 Usage 日志（网关上报）")
    @PostMapping
    public Result<Void> receiveUsageLog(@RequestBody UsageLogDTO dto) {
        UsageLogRecord record = new UsageLogRecord();
        record.setTimestamp(dto.getTimestamp());
        record.setTraceId(dto.getTraceId());
        record.setRequestId(dto.getRequestId());
        record.setTenantId(dto.getTenantId());
        record.setAppId(dto.getAppId());
        record.setUserId(dto.getUserId());
        record.setModel(dto.getModel());
        record.setProvider(dto.getProvider());
        record.setPromptTokens(dto.getPromptTokens());
        record.setCompletionTokens(dto.getCompletionTokens());
        record.setTotalTokens(dto.getTotalTokens());
        record.setCacheCreationTokens(dto.getCacheCreationTokens());
        record.setCacheReadTokens(dto.getCacheReadTokens());
        record.setStatus(dto.getStatus());
        record.setLatencyMs(dto.getLatencyMs());
        record.setTtftMs(dto.getTtftMs());
        record.setTpotMs(dto.getTpotMs());
        record.setErrorCode(dto.getErrorCode());
        record.setErrorMessage(dto.getErrorMessage());
        // 提取请求/响应内容摘要（前 500 字符）
        record.setRequestContent(truncate(dto.getRequestContent(), 500));
        record.setResponseContent(truncate(dto.getResponseContent(), 500));
        usageLogRecordService.saveAndIndex(record);
        return Result.success();
    }

    /**
     * 截断字符串到指定长度
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    @Operation(summary = "分页查询日志", description = "支持按时间、模型、状态、租户、traceId 筛选")
    @GetMapping
    public Result<Map<String, Object>> queryLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String traceId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        LambdaQueryWrapper<UsageLogRecord> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(UsageLogRecord::getTimestamp, startTime);
        if (endTime   != null) wrapper.le(UsageLogRecord::getTimestamp, endTime);
        if (model     != null && !model.isBlank())    wrapper.eq(UsageLogRecord::getModel, model);
        if (status    != null && !status.isBlank())   wrapper.eq(UsageLogRecord::getStatus, status);
        if (tenantId  != null && !tenantId.isBlank()) wrapper.eq(UsageLogRecord::getTenantId, tenantId);
        if (traceId   != null && !traceId.isBlank())  wrapper.eq(UsageLogRecord::getTraceId, traceId);
        wrapper.orderByDesc(UsageLogRecord::getTimestamp);

        IPage<UsageLogRecord> result = usageLogRecordService.page(new Page<>(page, pageSize), wrapper);

        Map<String, Object> resp = new HashMap<>();
        resp.put("list",     result.getRecords());
        resp.put("total",    result.getTotal());
        resp.put("page",     page);
        resp.put("pageSize", pageSize);
        resp.put("pages",    result.getPages());
        return Result.success(resp);
    }

    @Operation(summary = "日志统计", description = "按时间段统计总量、成功率、平均延迟、Token 消耗")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String tenantId) {

        LambdaQueryWrapper<UsageLogRecord> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) wrapper.ge(UsageLogRecord::getTimestamp, startTime);
        if (endTime   != null) wrapper.le(UsageLogRecord::getTimestamp, endTime);
        if (tenantId  != null && !tenantId.isBlank()) wrapper.eq(UsageLogRecord::getTenantId, tenantId);

        var logs = usageLogRecordService.list(wrapper);
        long total   = logs.size();
        long success = logs.stream().filter(l -> "success".equalsIgnoreCase(l.getStatus())).count();
        long avgLatency = total > 0 ? (long) logs.stream().mapToLong(l -> l.getLatencyMs() != null ? l.getLatencyMs() : 0).average().orElse(0) : 0;
        long avgTtft    = total > 0 ? (long) logs.stream().filter(l -> l.getTtftMs() != null).mapToLong(UsageLogRecord::getTtftMs).average().orElse(0) : 0;
        long totalTokens = logs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests",   total);
        stats.put("successRequests", success);
        stats.put("failedRequests",  total - success);
        stats.put("successRate",     total > 0 ? String.format("%.2f%%", success * 100.0 / total) : "0%");
        stats.put("avgLatencyMs",    avgLatency);
        stats.put("avgTtftMs",       avgTtft);
        stats.put("totalTokens",     totalTokens);
        return Result.success(stats);
    }
}
