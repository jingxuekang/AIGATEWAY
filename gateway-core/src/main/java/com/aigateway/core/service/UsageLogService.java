package com.aigateway.core.service;

import com.aigateway.common.context.ApiKeyContext;
import com.aigateway.common.dto.UsageLogDTO;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.util.TraceIdUtil;
import com.aigateway.core.client.AdminClient;
import com.aigateway.core.metrics.LlmMetrics;
import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageLogService {

    private final AdminClient adminClient;
    private final LlmMetrics llmMetrics;
    private final ObjectMapper objectMapper;
    private final PiiMaskService piiMaskService;

    public void logUsage(ChatRequest request,
                         ChatResponse response,
                         UsageLogContext ctx) {
        UsageLogDTO dto = new UsageLogDTO();
        dto.setTimestamp(LocalDateTime.now());
        dto.setTraceId(TraceIdUtil.getTraceId());
        dto.setModel(request.getModel());
        dto.setLatencyMs(ctx.getLatencyMs());
        dto.setTtftMs(ctx.getTtftMs());
        dto.setTpotMs(ctx.getTpotMs());
        dto.setProvider(ctx.getProviderName());
        dto.setStatus(ctx.getStatus());
        dto.setRequestId(ctx.getRequestId());
        dto.setErrorCode(ctx.getErrorCode());
        dto.setErrorMessage(ctx.getErrorMessage());

        // 提取请求内容摘要（前 500 字符）
        dto.setRequestContent(extractRequestContent(request));
        // 提取响应内容摘要（前 500 字符）
        dto.setResponseContent(extractResponseContent(response));

        ApiKeyInfo apiKeyInfo = ApiKeyContext.get();
        if (apiKeyInfo != null) {
            dto.setTenantId(apiKeyInfo.getTenantId());
            dto.setAppId(apiKeyInfo.getAppId());
            dto.setUserId(apiKeyInfo.getUserId() != null ? apiKeyInfo.getUserId().toString() : null);
        }

        int promptTokens = 0, completionTokens = 0;
        if (response != null && response.getUsage() != null) {
            ChatResponse.Usage u = response.getUsage();
            promptTokens = u.getPromptTokens() != null ? u.getPromptTokens() : 0;
            completionTokens = u.getCompletionTokens() != null ? u.getCompletionTokens() : 0;
            dto.setPromptTokens(promptTokens);
            dto.setCompletionTokens(completionTokens);
            dto.setTotalTokens(u.getTotalTokens() != null ? u.getTotalTokens() : 0);
            dto.setCacheCreationTokens(u.getCacheCreationTokens());
            dto.setCacheReadTokens(u.getCacheReadTokens());
        }

        log.info("Usage: model={} provider={} status={} latency={}ms ttft={}ms tpot={}ms prompt={} completion={}",
                dto.getModel(), dto.getProvider(), dto.getStatus(),
                dto.getLatencyMs(), dto.getTtftMs(), dto.getTpotMs(),
                promptTokens, completionTokens);

        llmMetrics.record(request.getModel(), ctx.getProviderName(), ctx.getStatus(),
                ctx.getLatencyMs(), promptTokens, completionTokens);

        try {
            adminClient.sendUsageLog(dto);
        } catch (Exception e) {
            log.error("Failed to send usage log", e);
        }
    }

    /**
     * 提取请求内容摘要（PII 脱敏 + 截断 500 字符）
     */
    private String extractRequestContent(ChatRequest request) {
        if (request == null) return null;
        try {
            String json = objectMapper.writeValueAsString(request);
            return piiMaskService.maskForLog(json, 500);
        } catch (Exception e) {
            log.debug("Failed to serialize request", e);
            return null;
        }
    }

    /**
     * 提取响应内容摘要（PII 脱敏 + 截断 500 字符）
     */
    private String extractResponseContent(ChatResponse response) {
        if (response == null) return null;
        try {
            String json = objectMapper.writeValueAsString(response);
            return piiMaskService.maskForLog(json, 500);
        } catch (Exception e) {
            log.debug("Failed to serialize response", e);
            return null;
        }
    }
}
