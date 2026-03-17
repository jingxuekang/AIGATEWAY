package com.aigateway.common.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UsageLogDTO {
    private String id;
    private LocalDateTime timestamp;
    private String traceId;
    private String requestId;
    private String tenantId;
    private String appId;
    private String userId;
    private String model;
    private String provider;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer cacheCreationTokens;
    private Integer cacheReadTokens;
    private String status;
    private Long latencyMs;
    private Long ttftMs;
    private Long tpotMs;
    private String errorCode;
    private String errorMessage;
    private String requestContent;    // 请求内容摘要（前 500 字符）
    private String responseContent;   // 响应内容摘要（前 500 字符）
}
