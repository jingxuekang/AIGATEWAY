package com.aigateway.admin.controller;

import com.aigateway.admin.entity.UsageLogRecord;
import com.aigateway.admin.entity.User;
import com.aigateway.admin.service.UsageLogRecordService;
import com.aigateway.admin.service.UserService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务日志（仿 New-API 日志明细视图）
 *
 * 说明：
 * - 你们的“调用明细”真实数据写在 {@code usage_log}（由 gateway-core 上报）。
 * - 本接口不改调用日志链路，仅把调用明细映射为 New-API 风格的“任务日志/日志明细”字段返回，
 *   让前端任务日志页展示更接近 new-api 的 Log Module。
 */
@Tag(name = "Task Management", description = "任务日志")
@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final UsageLogRecordService usageLogRecordService;
    private final UserService userService;

    private static void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }

    private static LocalDateTime parseDateTimeParam(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String t = s.trim();
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            // 兼容前端 dayjs.toISOString()：2026-03-20T02:17:55.000Z
            try {
                return java.time.OffsetDateTime.parse(t).toLocalDateTime();
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    /**
     * 将 usage_log 映射为 New-API 风格的日志字段（近似）。
     *
     * 注：你们当前 usage_log 没有落地 request_header/response_header/ip/key_id/channel_id/out_url 等字段，
     * 所以这些字段在返回中会以 null 或占位形式呈现。
     */
    private static Map<String, Object> toNewApiLikeLogRow(UsageLogRecord log) {
        Map<String, Object> row = new HashMap<>();

        // New-API: id / user_id / created_at / updated_at
        row.put("id", log.getId());
        row.put("userId", log.getUserId());
        row.put("createdAt", log.getTimestamp());
        row.put("updatedAt", log.getTimestamp());

        // New-API: key_id（你们当前 usage_log 里没有保存 api key id）
        row.put("keyId", null);

        // New-API: model / channel_id / channel_name
        row.put("model", log.getModel());
        String provider = log.getProvider();
        if (provider != null && provider.contains(":")) {
            int idx = provider.lastIndexOf(':');
            row.put("channelName", provider.substring(idx + 1));
            row.put("channelId", provider.substring(0, idx));
        } else {
            row.put("channelName", provider);
            row.put("channelId", provider);
        }

        // New-API: request_url / request_method（近似）
        row.put("requestUrl", "/v1/chat/completions");
        row.put("requestMethod", "POST");
        row.put("requestHeaders", null);
        row.put("requestBody", log.getRequestContent());
        row.put("responseHeaders", null);
        row.put("responseBody", log.getResponseContent());

        // New-API: tokens / quota / cost_time
        row.put("promptTokens", log.getPromptTokens());
        row.put("completionTokens", log.getCompletionTokens());
        row.put("totalTokens", log.getTotalTokens());
        row.put("quota", log.getTotalTokens()); // 近似：把 total_tokens 当作额度扣减量展示
        row.put("costTimeMs", log.getLatencyMs());

        // New-API: ip（你们当前 usage_log 没有保存 ip，前端可按需展示占位）
        row.put("ip", "127.0.0.1");

        // New-API: status / error
        String originStatus = log.getStatus();
        boolean ok = originStatus != null && "success".equalsIgnoreCase(originStatus);
        row.put("status", ok ? "success" : "failed");
        row.put("error", StringUtils.hasText(log.getErrorMessage()) ? log.getErrorMessage() : log.getErrorCode());

        // 额外可用：延迟、TTFT/TPOT（便于新 UI 更靠近 new-api）
        row.put("latencyMs", log.getLatencyMs());
        row.put("ttftMs", log.getTtftMs());
        row.put("tpotMs", log.getTpotMs());
        row.put("traceId", log.getTraceId());
        row.put("requestId", log.getRequestId());

        return row;
    }

    @Operation(summary = "分页查询任务日志（仅 admin）")
    @GetMapping
    public Result<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpServletRequest request) {
        requireAdmin(request);

        LocalDateTime from = parseDateTimeParam(startTime);
        LocalDateTime to = parseDateTimeParam(endTime);

        LambdaQueryWrapper<UsageLogRecord> wrapper = new LambdaQueryWrapper<>();
        if (from != null) wrapper.ge(UsageLogRecord::getTimestamp, from);
        if (to != null) wrapper.le(UsageLogRecord::getTimestamp, to);
        if (StringUtils.hasText(model)) wrapper.eq(UsageLogRecord::getModel, model.trim());
        if (StringUtils.hasText(tenantId)) wrapper.eq(UsageLogRecord::getTenantId, tenantId.trim());
        if (StringUtils.hasText(status)) wrapper.eq(UsageLogRecord::getStatus, status.trim());

        wrapper.orderByDesc(UsageLogRecord::getTimestamp);

        IPage<UsageLogRecord> pg = usageLogRecordService.page(
                new Page<>(page != null && page > 0 ? page : 1, pageSize != null && pageSize > 0 ? pageSize : 20),
                wrapper
        );

        // 填充 username（近似 new-api：user_id/username 一起展示）
        List<UsageLogRecord> records = pg.getRecords();
        List<Long> userIds = records.stream()
                .map(r -> {
                    try {
                        return r.getUserId() != null ? Long.parseLong(r.getUserId()) : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(id -> id != null).distinct().toList();
        if (!userIds.isEmpty()) {
            Map<Long, String> userMap = userService.listByIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, User::getUsername));
            records.forEach(r -> {
                try {
                    if (r.getUserId() != null) {
                        Long uid = Long.parseLong(r.getUserId());
                        r.setUsername(userMap.getOrDefault(uid, r.getUserId()));
                    }
                } catch (Exception ignored) {}
            });
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (UsageLogRecord log : records) {
            Map<String, Object> row = toNewApiLikeLogRow(log);
            row.put("username", log.getUsername());
            list.add(row);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", pg.getTotal());
        result.put("page", pg.getCurrent());
        result.put("pageSize", pg.getSize());
        return Result.success(result);
    }
}
