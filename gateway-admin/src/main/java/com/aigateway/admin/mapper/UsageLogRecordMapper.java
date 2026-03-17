package com.aigateway.admin.mapper;

import com.aigateway.admin.entity.UsageLogRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface UsageLogRecordMapper extends BaseMapper<UsageLogRecord> {

    /** 总请求数 */
    @Select("SELECT COUNT(*) FROM usage_log WHERE deleted = 0")
    Long countTotal();

    /** 成功请求数 */
    @Select("SELECT COUNT(*) FROM usage_log WHERE status = 'success' AND deleted = 0")
    Long countSuccess();

    /** 总 token 消耗 */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log WHERE deleted = 0")
    Long sumTotalTokens();

    /** 今日请求数 */
    @Select("SELECT COUNT(*) FROM usage_log WHERE DATE(timestamp) = CURDATE() AND deleted = 0")
    Long countToday();

    /** 今日 token 消耗 */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log WHERE DATE(timestamp) = CURDATE() AND deleted = 0")
    Long sumTodayTokens();

    /** 平均延迟（毫秒） */
    @Select("SELECT COALESCE(AVG(latency_ms), 0) FROM usage_log WHERE status = 'success' AND deleted = 0")
    Double avgLatencyMs();

    /** 平均 TTFT（毫秒） */
    @Select("SELECT COALESCE(AVG(ttft_ms), 0) FROM usage_log WHERE status = 'success' AND ttft_ms > 0 AND deleted = 0")
    Double avgTtftMs();

    /** 按模型统计 token 消耗（Top 10） */
    @Select("SELECT model, COALESCE(SUM(total_tokens), 0) AS tokens, COUNT(*) AS requests " +
            "FROM usage_log WHERE deleted = 0 GROUP BY model ORDER BY tokens DESC LIMIT 10")
    List<Map<String, Object>> tokensByModel();

    /** 按 provider 统计请求数和 token 数 */
    @Select("SELECT provider, COUNT(*) AS requests, COALESCE(SUM(total_tokens), 0) AS tokens " +
            "FROM usage_log WHERE deleted = 0 GROUP BY provider ORDER BY requests DESC")
    List<Map<String, Object>> statsByProvider();

    /** 近 7 天每日请求数和 token 数 */
    @Select("SELECT DATE(timestamp) AS date, COUNT(*) AS requests, COALESCE(SUM(total_tokens), 0) AS tokens " +
            "FROM usage_log WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND deleted = 0 " +
            "GROUP BY DATE(timestamp) ORDER BY date ASC")
    List<Map<String, Object>> dailyStats7Days();

    /** 最近 10 条调用记录 */
    @Select("SELECT id, timestamp, model, provider, status, total_tokens AS totalTokens, latency_ms AS latencyMs " +
            "FROM usage_log WHERE deleted = 0 ORDER BY timestamp DESC LIMIT 10")
    List<Map<String, Object>> recentLogs();
}
