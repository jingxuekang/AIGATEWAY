package com.aigateway.core.service;

import lombok.Builder;
import lombok.Data;

/**
 * 日志上报上下文对象，替代 logUsage 方法的 10 个散参数
 */
@Data
@Builder
public class UsageLogContext {
    private String requestId;
    private String providerName;
    private String status;
    private String errorCode;
    private String errorMessage;
    private long latencyMs;
    private long ttftMs;
    private long tpotMs;
}
