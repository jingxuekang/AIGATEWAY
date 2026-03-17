package com.aigateway.admin.service;

import com.aigateway.admin.mapper.ApiKeyMapper;
import com.aigateway.admin.mapper.ModelMapper;
import com.aigateway.admin.mapper.UsageLogRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard 统计服务
 * 从 usage_log 表真实聚合数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ApiKeyMapper apiKeyMapper;
    private final ModelMapper modelMapper;
    private final UsageLogRecordMapper usageLogRecordMapper;

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // API Key 总数
        stats.put("totalKeys", safeCount(apiKeyMapper.selectCount(null)));

        // 模型总数
        stats.put("totalModels", safeCount(modelMapper.selectCount(null)));

        // 总请求数
        long totalRequests = safeCount(usageLogRecordMapper.countTotal());
        stats.put("totalRequests", totalRequests);

        // 成功请求数
        long successRequests = safeCount(usageLogRecordMapper.countSuccess());
        stats.put("successRequests", successRequests);

        // 成功率（百分比，保留一位小数）
        double successRate = totalRequests > 0
                ? Math.round(successRequests * 1000.0 / totalRequests) / 10.0
                : 100.0;
        stats.put("successRate", successRate);

        // 总 token 消耗
        stats.put("totalTokens", safeCount(usageLogRecordMapper.sumTotalTokens()));

        // 今日请求数
        stats.put("todayRequests", safeCount(usageLogRecordMapper.countToday()));

        // 今日 token 消耗
        stats.put("todayTokens", safeCount(usageLogRecordMapper.sumTodayTokens()));

        // 平均延迟（毫秒）
        Double avgLatency = usageLogRecordMapper.avgLatencyMs();
        stats.put("avgLatencyMs", avgLatency != null ? Math.round(avgLatency) : 0);

        // 平均 TTFT（毫秒）
        Double avgTtft = usageLogRecordMapper.avgTtftMs();
        stats.put("avgTtftMs", avgTtft != null ? Math.round(avgTtft) : 0);

        // 最近 10 条调用记录
        try {
            List<Map<String, Object>> recentLogs = usageLogRecordMapper.recentLogs();
            stats.put("recentLogs", recentLogs);
        } catch (Exception e) {
            log.warn("Failed to get recent logs", e);
            stats.put("recentLogs", List.of());
        }

        // 按模型统计 token 消耗（Top 10）
        try {
            stats.put("tokensByModel", usageLogRecordMapper.tokensByModel());
        } catch (Exception e) {
            log.warn("Failed to get tokens by model", e);
            stats.put("tokensByModel", List.of());
        }

        // 按 provider 统计
        try {
            stats.put("statsByProvider", usageLogRecordMapper.statsByProvider());
        } catch (Exception e) {
            log.warn("Failed to get stats by provider", e);
            stats.put("statsByProvider", List.of());
        }

        // 近 7 天每日统计
        try {
            stats.put("dailyStats", usageLogRecordMapper.dailyStats7Days());
        } catch (Exception e) {
            log.warn("Failed to get daily stats", e);
            stats.put("dailyStats", List.of());
        }

        log.info("Dashboard: totalRequests={}, totalTokens={}, successRate={}%, avgLatency={}ms",
                totalRequests, stats.get("totalTokens"), successRate, stats.get("avgLatencyMs"));
        return stats;
    }

    private long safeCount(Long val) {
        return val != null ? val : 0L;
    }
}
