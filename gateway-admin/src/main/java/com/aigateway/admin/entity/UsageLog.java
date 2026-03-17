package com.aigateway.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 使用日志实体（ES 文档）
 */
@Data
public class UsageLog {
    
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
}
