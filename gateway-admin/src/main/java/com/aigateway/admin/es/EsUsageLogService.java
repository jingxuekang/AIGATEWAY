package com.aigateway.admin.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.aigateway.admin.entity.UsageLogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

/**
 * ES 日志写入服务
 * 异步写入，不影响主流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsUsageLogService {

    private static final String INDEX = "usage_log";

    private final ElasticsearchClient esClient;

    /**
     * 异步将 UsageLogRecord 写入 ES
     */
    @Async
    public void indexLog(UsageLogRecord record) {
        if (record == null) return;
        try {
            UsageLogDocument doc = UsageLogDocument.builder()
                    .id(record.getId())
                    .timestamp(record.getTimestamp() != null 
                            ? record.getTimestamp().atZone(ZoneId.systemDefault()).toInstant() 
                            : null)
                    .traceId(record.getTraceId())
                    .requestId(record.getRequestId())
                    .tenantId(record.getTenantId())
                    .appId(record.getAppId())
                    .userId(record.getUserId())
                    .model(record.getModel())
                    .provider(record.getProvider())
                    .promptTokens(record.getPromptTokens())
                    .completionTokens(record.getCompletionTokens())
                    .totalTokens(record.getTotalTokens())
                    .status(record.getStatus())
                    .latencyMs(record.getLatencyMs())
                    .ttftMs(record.getTtftMs())
                    .tpotMs(record.getTpotMs())
                    .errorCode(record.getErrorCode())
                    .errorMessage(record.getErrorMessage())
                    .requestContent(record.getRequestContent())
                    .responseContent(record.getResponseContent())
                    .build();

            IndexRequest<UsageLogDocument> request = IndexRequest.of(r -> r
                    .index(INDEX)
                    .id(record.getId() != null ? record.getId().toString() : null)
                    .document(doc));

            esClient.index(request);
            log.debug("ES index success: id={}, model={}, status={}",
                    record.getId(), record.getModel(), record.getStatus());
        } catch (Exception e) {
            log.warn("ES index failed: id={}, error={}", record.getId(), e.getMessage());
        }
    }
}
