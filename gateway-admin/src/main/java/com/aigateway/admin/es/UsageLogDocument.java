package com.aigateway.admin.es;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

/**
 * ES 日志文档（索引：usage_log）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageLogDocument {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant timestamp;

    @JsonProperty("trace_id")
    private String traceId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("user_id")
    private String userId;

    private String model;

    private String provider;

    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    private String status;

    @JsonProperty("latency_ms")
    private Long latencyMs;

    @JsonProperty("ttft_ms")
    private Long ttftMs;

    @JsonProperty("tpot_ms")
    private Long tpotMs;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("request_content")
    private String requestContent;    // 请求内容摘要（前 500 字符）

    @JsonProperty("response_content")
    private String responseContent;   // 响应内容摘要（前 500 字符）
}
