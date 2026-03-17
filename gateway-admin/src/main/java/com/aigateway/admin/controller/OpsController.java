package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Model;
import com.aigateway.admin.entity.UsageLogRecord;
import com.aigateway.admin.service.ModelService;
import com.aigateway.admin.service.UsageLogRecordService;
import com.aigateway.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Ops API", description = "运维平台对接接口")
@RestController
@RequestMapping("/api/ops")
@RequiredArgsConstructor
public class OpsController {

    private final ModelService modelService;
    private final UsageLogRecordService usageLogRecordService;

    @Operation(summary = "CMDB - 模型资产列表")
    @GetMapping("/cmdb/models")
    public Result<List<Map<String, Object>>> cmdbModels() {
        List<Model> models = modelService.list(
                new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1));
        List<Map<String, Object>> assets = models.stream().map(m -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", m.getId());
            a.put("modelName", m.getModelName());
            a.put("modelVersion", m.getModelVersion());
            a.put("provider", m.getProvider());
            a.put("status", m.getStatus());
            a.put("maxTokens", m.getMaxTokens());
            a.put("supportStream", m.getSupportStream());
            a.put("inputPrice", m.getInputPrice());
            a.put("outputPrice", m.getOutputPrice());
            return a;
        }).collect(Collectors.toList());
        return Result.success(assets);
    }

    @Operation(summary = "链路健康 Ping")
    @GetMapping("/health/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "UP");
        resp.put("timestamp", LocalDateTime.now().toString());
        resp.put("service", "ai-gateway-admin");
        return Result.success(resp);
    }

    @Operation(summary = "成本统计")
    @GetMapping("/cost/summary")
    public Result<Map<String, Object>> costSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String appId) {

        LambdaQueryWrapper<UsageLogRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageLogRecord::getStatus, "success");
        if (startTime != null) wrapper.ge(UsageLogRecord::getTimestamp, startTime);
        if (endTime != null) wrapper.le(UsageLogRecord::getTimestamp, endTime);
        if (tenantId != null && !tenantId.isBlank()) wrapper.eq(UsageLogRecord::getTenantId, tenantId);
        if (appId != null && !appId.isBlank()) wrapper.eq(UsageLogRecord::getAppId, appId);

        List<UsageLogRecord> logs = usageLogRecordService.list(wrapper);

        Map<String, Map<String, Object>> byModel = new LinkedHashMap<>();
        for (UsageLogRecord log : logs) {
            String key = log.getModel() != null ? log.getModel() : "unknown";
            byModel.computeIfAbsent(key, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("model", k); m.put("requests", 0L);
                m.put("promptTokens", 0L); m.put("completionTokens", 0L); m.put("totalTokens", 0L);
                return m;
            });
            Map<String, Object> stat = byModel.get(key);
            stat.put("requests", (Long) stat.get("requests") + 1);
            stat.put("promptTokens", (Long) stat.get("promptTokens") + (log.getPromptTokens() != null ? log.getPromptTokens() : 0));
            stat.put("completionTokens", (Long) stat.get("completionTokens") + (log.getCompletionTokens() != null ? log.getCompletionTokens() : 0));
            stat.put("totalTokens", (Long) stat.get("totalTokens") + (log.getTotalTokens() != null ? log.getTotalTokens() : 0));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalRequests", logs.size());
        result.put("totalTokens", logs.stream().mapToLong(l -> l.getTotalTokens() != null ? l.getTotalTokens() : 0).sum());
        result.put("byModel", new ArrayList<>(byModel.values()));
        result.put("queryRange", Map.of("start", startTime != null ? startTime.toString() : "", "end", endTime != null ? endTime.toString() : ""));
        return Result.success(result);
    }

    @Operation(summary = "延迟监控")
    @GetMapping("/metrics/latency")
    public Result<Map<String, Object>> latencyMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        LambdaQueryWrapper<UsageLogRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UsageLogRecord::getStatus, "success");
        wrapper.isNotNull(UsageLogRecord::getLatencyMs);
        if (startTime != null) wrapper.ge(UsageLogRecord::getTimestamp, startTime);
        if (endTime != null) wrapper.le(UsageLogRecord::getTimestamp, endTime);

        List<long[]> latencies = usageLogRecordService.list(wrapper).stream()
                .map(l -> new long[]{l.getLatencyMs(), l.getTtftMs() != null ? l.getTtftMs() : 0L})
                .sorted(Comparator.comparingLong(a -> a[0]))
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", latencies.size());
        if (!latencies.isEmpty()) {
            result.put("p50_latency_ms", latencies.get((int) (latencies.size() * 0.50))[0]);
            result.put("p95_latency_ms", latencies.get((int) (latencies.size() * 0.95))[0]);
            result.put("p99_latency_ms", latencies.get((int) (latencies.size() * 0.99))[0]);
            result.put("p50_ttft_ms", latencies.get((int) (latencies.size() * 0.50))[1]);
            result.put("p95_ttft_ms", latencies.get((int) (latencies.size() * 0.95))[1]);
        }
        return Result.success(result);
    }
}
