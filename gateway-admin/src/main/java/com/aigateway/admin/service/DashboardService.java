package com.aigateway.admin.service;

import com.aigateway.admin.mapper.ApiKeyMapper;
import com.aigateway.admin.mapper.ModelMapper;
import com.aigateway.admin.mapper.UsageLogRecordMapper;
import com.aigateway.admin.entity.Provider;
import com.aigateway.admin.entity.User;
import com.aigateway.admin.entity.AdminUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard 统计服务
 * 从 usage_log 表真实聚合数据
 * userId != null 时只统计该用户的数据（普通用户），null 时统计全局（admin）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApiKeyMapper apiKeyMapper;
    private final ModelMapper modelMapper;
    private final UsageLogRecordMapper usageLogRecordMapper;
    private final UserService userService;
    private final AdminUserService adminUserService;
    private final ProviderService providerService;

    public Map<String, Object> getStatistics(String userId) {
        Map<String, Object> stats = new HashMap<>();
        boolean filtered = userId != null && !userId.isBlank();

        // API Key 总数（普通用户只看自己）
        stats.put("totalKeys", filtered
                ? safeCount(apiKeyMapper.countByUser(userId))
                : safeCount(apiKeyMapper.selectCount(null)));

        // 模型总数
        stats.put("totalModels", safeCount(modelMapper.selectCount(null)));

        // 总请求数
        long totalRequests = filtered
                ? safeCount(usageLogRecordMapper.countTotalByUser(userId))
                : safeCount(usageLogRecordMapper.countTotal());
        stats.put("totalRequests", totalRequests);

        // 成功请求数
        long successRequests = filtered
                ? safeCount(usageLogRecordMapper.countSuccessByUser(userId))
                : safeCount(usageLogRecordMapper.countSuccess());
        stats.put("successRequests", successRequests);

        // 成功率
        double successRate = totalRequests > 0
                ? Math.round(successRequests * 1000.0 / totalRequests) / 10.0
                : 100.0;
        stats.put("successRate", successRate);

        // 总 token 消耗
        stats.put("totalTokens", filtered
                ? safeCount(usageLogRecordMapper.sumTotalTokensByUser(userId))
                : safeCount(usageLogRecordMapper.sumTotalTokens()));

        // 今日请求数
        stats.put("todayRequests", filtered
                ? safeCount(usageLogRecordMapper.countTodayByUser(userId))
                : safeCount(usageLogRecordMapper.countToday()));

        // 今日 token 消耗
        stats.put("todayTokens", filtered
                ? safeCount(usageLogRecordMapper.sumTodayTokensByUser(userId))
                : safeCount(usageLogRecordMapper.sumTodayTokens()));

        // 平均延迟 / TTFT（普通用户仅看自己）
        Double avgLatency = filtered
                ? usageLogRecordMapper.avgLatencyMsByUser(userId)
                : usageLogRecordMapper.avgLatencyMs();
        stats.put("avgLatencyMs", avgLatency != null ? Math.round(avgLatency) : 0);

        Double avgTtft = filtered
                ? usageLogRecordMapper.avgTtftMsByUser(userId)
                : usageLogRecordMapper.avgTtftMs();
        stats.put("avgTtftMs", avgTtft != null ? Math.round(avgTtft) : 0);

        // 最近 10 条调用记录
        try {
            List<Map<String, Object>> recentLogs = filtered
                    ? usageLogRecordMapper.recentLogsByUser(userId)
                    : usageLogRecordMapper.recentLogs();
            stats.put("recentLogs", recentLogs);
        } catch (Exception e) {
            log.warn("Failed to get recent logs", e);
            stats.put("recentLogs", List.of());
        }

        // 以下统计仅全局（admin）返回
        if (!filtered) {
            try {
                stats.put("tokensByModel", usageLogRecordMapper.tokensByModel());
            } catch (Exception e) {
                log.warn("Failed to get tokens by model", e);
                stats.put("tokensByModel", List.of());
            }
            try {
                List<Map<String, Object>> byProv = usageLogRecordMapper.statsByProvider();
                enrichProviderDisplayNames(byProv);
                stats.put("statsByProvider", byProv);
            } catch (Exception e) {
                log.warn("Failed to get stats by provider", e);
                stats.put("statsByProvider", List.of());
            }
            try {
                stats.put("dailyStats", usageLogRecordMapper.dailyStats7Days());
            } catch (Exception e) {
                log.warn("Failed to get daily stats", e);
                stats.put("dailyStats", List.of());
            }
            try {
                stats.put("topUsers", withUsername(usageLogRecordMapper.topUsers7d()));
            } catch (Exception e) {
                log.warn("Failed to get top users", e);
                stats.put("topUsers", List.of());
            }
        } else {
            try {
                stats.put("tokensByModel", usageLogRecordMapper.tokensByModelByUser(userId));
            } catch (Exception e) {
                log.warn("Failed to get tokens by model (user)", e);
                stats.put("tokensByModel", List.of());
            }
            try {
                List<Map<String, Object>> byProvU = usageLogRecordMapper.statsByProviderByUser(userId);
                enrichProviderDisplayNames(byProvU);
                stats.put("statsByProvider", byProvU);
            } catch (Exception e) {
                log.warn("Failed to get stats by provider (user)", e);
                stats.put("statsByProvider", List.of());
            }
            try {
                stats.put("dailyStats", usageLogRecordMapper.dailyStats7DaysByUser(userId));
            } catch (Exception e) {
                log.warn("Failed to get daily stats (user)", e);
                stats.put("dailyStats", List.of());
            }
            stats.put("topUsers", List.of());
        }

        log.info("Dashboard[userId={}]: totalRequests={}, totalTokens={}, successRate={}%",
                userId, totalRequests, stats.get("totalTokens"), successRate);
        return stats;
    }

    private long safeCount(Long val) {
        return val != null ? val : 0L;
    }

    private List<Map<String, Object>> withUsername(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        List<Long> ids = rows.stream()
                .map(m -> m.get("userId"))
                .filter(v -> v != null)
                .map(v -> {
                    try { return Long.parseLong(String.valueOf(v)); } catch (Exception e) { return null; }
                })
                .filter(v -> v != null)
                .distinct()
                .toList();

        Map<Long, String> userMap = ids.isEmpty() ? Map.of() : userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        Map<Long, String> adminMap = ids.isEmpty() ? Map.of() : adminUserService.listByIds(ids).stream()
                .collect(Collectors.toMap(AdminUser::getId, AdminUser::getUsername));

        rows.forEach(m -> {
            String name = null;
            try {
                Long uid = Long.parseLong(String.valueOf(m.get("userId")));
                name = userMap.get(uid);
                if (name == null) name = adminMap.get(uid);
            } catch (Exception ignored) {
            }
            m.put("username", name != null ? name : "（用户不存在或已删除）");
        });
        return rows;
    }

    /**
     * 与「渠道管理」列表一致：用 provider 元数据表的展示名（name），便于和 channel.provider 代码对齐。
     * {@code provider} 列仍为日志里的归一化 code（如 deepseek / azure-openai），{@code providerLabel} 为展示名。
     */
    private void enrichProviderDisplayNames(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return;
        Map<String, String> codeToName = providerService.listAll().stream()
                .filter(p -> p.getCode() != null && !p.getCode().isBlank())
                .collect(Collectors.toMap(
                        p -> p.getCode().toLowerCase(),
                        Provider::getName,
                        (a, b) -> a));
        for (Map<String, Object> row : rows) {
            String code = row.get("provider") != null ? String.valueOf(row.get("provider")) : "";
            String key = code.toLowerCase();
            String label = codeToName.get(key);
            // 历史日志里熔断器等可能写 azure-openai，与渠道里选的 code=azure 对齐到同一展示名
            if (label == null && "azure-openai".equalsIgnoreCase(code)) {
                label = codeToName.get("azure");
            }
            row.put("providerLabel", label != null ? label : code);
        }
    }
}
