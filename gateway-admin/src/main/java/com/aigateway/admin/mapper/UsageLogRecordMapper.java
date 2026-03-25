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
    @Select("SELECT COUNT(*) FROM usage_log")
    Long countTotal();

    /** 成功请求数 */
    @Select("SELECT COUNT(*) FROM usage_log WHERE status = 'success'")
    Long countSuccess();

    /** 总 token 消耗 */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log")
    Long sumTotalTokens();

    /** 今日请求数 */
    @Select("SELECT COUNT(*) FROM usage_log WHERE DATE(timestamp) = CURDATE()")
    Long countToday();

    /** 今日 token 消耗 */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log WHERE DATE(timestamp) = CURDATE()")
    Long sumTodayTokens();

    /** 平均延迟（毫秒） */
    @Select("SELECT COALESCE(AVG(latency_ms), 0) FROM usage_log WHERE status = 'success'")
    Double avgLatencyMs();

    /** 平均 TTFT（毫秒） */
    @Select("SELECT COALESCE(AVG(ttft_ms), 0) FROM usage_log WHERE status = 'success' AND ttft_ms > 0")
    Double avgTtftMs();

    @Select("SELECT COALESCE(AVG(latency_ms), 0) FROM usage_log WHERE status = 'success' AND user_id = #{userId}")
    Double avgLatencyMsByUser(String userId);

    @Select("SELECT COALESCE(AVG(ttft_ms), 0) FROM usage_log WHERE status = 'success' AND ttft_ms > 0 AND user_id = #{userId}")
    Double avgTtftMsByUser(String userId);

    /** 按模型统计 token 消耗（Top 10，仅统计 model 表中已发布且未删除的模型） */
    @Select("SELECT u.model AS model, COALESCE(SUM(u.total_tokens), 0) AS tokens, COUNT(*) AS requests " +
            "FROM usage_log u " +
            "INNER JOIN model m ON m.model_name = u.model AND m.deleted = 0 AND m.status = 1 " +
            "GROUP BY u.model ORDER BY tokens DESC LIMIT 10")
    List<Map<String, Object>> tokensByModel();

    /** 当前用户在已发布模型中的 Token 排行（个人看板） */
    @Select("SELECT u.model AS model, COALESCE(SUM(u.total_tokens), 0) AS tokens, COUNT(*) AS requests " +
            "FROM usage_log u " +
            "INNER JOIN model m ON m.model_name = u.model AND m.deleted = 0 AND m.status = 1 " +
            "WHERE u.user_id = #{userId} " +
            "GROUP BY u.model ORDER BY tokens DESC LIMIT 10")
    List<Map<String, Object>> tokensByModelByUser(String userId);

    /**
     * 按「厂商类型」聚合：动态渠道记为 channel:{id}:{provider}，静态 Bean 记为 deepseek/openai 等，
     * 统一按最后一段归并，避免同一渠道出现两行。
     */
    @Select("SELECT SUBSTRING_INDEX(provider, ':', -1) AS provider, COUNT(*) AS requests, " +
            "COALESCE(SUM(total_tokens), 0) AS tokens FROM usage_log " +
            "GROUP BY SUBSTRING_INDEX(provider, ':', -1) ORDER BY requests DESC")
    List<Map<String, Object>> statsByProvider();

    /** 个人维度：按归一化 provider 聚合 */
    @Select("SELECT SUBSTRING_INDEX(u.provider, ':', -1) AS provider, COUNT(*) AS requests, " +
            "COALESCE(SUM(u.total_tokens), 0) AS tokens FROM usage_log u " +
            "WHERE u.user_id = #{userId} " +
            "GROUP BY SUBSTRING_INDEX(u.provider, ':', -1) ORDER BY requests DESC")
    List<Map<String, Object>> statsByProviderByUser(String userId);

    /** 近 7 天每日请求数和 token 数 */
    @Select("SELECT DATE(timestamp) AS date, COUNT(*) AS requests, COALESCE(SUM(total_tokens), 0) AS tokens " +
            "FROM usage_log WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(timestamp) ORDER BY date ASC")
    List<Map<String, Object>> dailyStats7Days();

    /** 近 7 天每日请求（按用户） */
    @Select("SELECT DATE(timestamp) AS date, COUNT(*) AS requests, COALESCE(SUM(total_tokens), 0) AS tokens " +
            "FROM usage_log WHERE user_id = #{userId} AND timestamp >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "GROUP BY DATE(timestamp) ORDER BY date ASC")
    List<Map<String, Object>> dailyStats7DaysByUser(String userId);

    /** 最近 10 条调用记录 */
    @Select("SELECT id, timestamp, model, provider, status, total_tokens AS totalTokens, latency_ms AS latencyMs " +
            "FROM usage_log ORDER BY timestamp DESC LIMIT 10")
    List<Map<String, Object>> recentLogs();

    /** 总请求数（按用户过滤） */
    @Select("SELECT COUNT(*) FROM usage_log WHERE user_id = #{userId}")
    Long countTotalByUser(String userId);

    /** 成功请求数（按用户过滤） */
    @Select("SELECT COUNT(*) FROM usage_log WHERE status = 'success' AND user_id = #{userId}")
    Long countSuccessByUser(String userId);

    /** 总 token 消耗（按用户过滤） */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log WHERE user_id = #{userId}")
    Long sumTotalTokensByUser(String userId);

    /** 今日请求数（按用户过滤） */
    @Select("SELECT COUNT(*) FROM usage_log WHERE DATE(timestamp) = CURDATE() AND user_id = #{userId}")
    Long countTodayByUser(String userId);

    /** 今日 token 消耗（按用户过滤） */
    @Select("SELECT COALESCE(SUM(total_tokens), 0) FROM usage_log WHERE DATE(timestamp) = CURDATE() AND user_id = #{userId}")
    Long sumTodayTokensByUser(String userId);

    /** 最近 10 条调用记录（按用户过滤） */
    @Select("SELECT id, timestamp, model, provider, status, total_tokens AS totalTokens, latency_ms AS latencyMs " +
            "FROM usage_log WHERE user_id = #{userId} ORDER BY timestamp DESC LIMIT 10")
    List<Map<String, Object>> recentLogsByUser(String userId);

    /** 近 7 天 token 消耗 TOP 用户（排除无 user_id 的调用，如鉴权关闭或内部调用） */
    @Select("SELECT user_id AS userId, COUNT(*) AS requests, COALESCE(SUM(total_tokens),0) AS tokens " +
            "FROM usage_log WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) " +
            "AND user_id IS NOT NULL AND TRIM(user_id) <> '' " +
            "GROUP BY user_id ORDER BY tokens DESC LIMIT 10")
    List<Map<String, Object>> topUsers7d();

    /** 近 7 天 token 消耗 TOP 用户（按 user 过滤） */
    @Select("SELECT user_id AS userId, COUNT(*) AS requests, COALESCE(SUM(total_tokens),0) AS tokens " +
            "FROM usage_log WHERE timestamp >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) AND user_id = #{userId} " +
            "AND user_id IS NOT NULL AND TRIM(user_id) <> '' " +
            "GROUP BY user_id ORDER BY tokens DESC LIMIT 10")
    List<Map<String, Object>> topUsers7dByUser(String userId);
}
