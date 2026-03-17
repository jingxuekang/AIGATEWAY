package com.aigateway.common.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiKeyInfo {
    
    private Long id;
    
    private String keyValue;
    
    private String keyName;
    
    private Long userId;
    
    private String tenantId;
    
    private String appId;
    
    private Integer status;
    
    private LocalDateTime expireTime;
    
    private List<String> allowedModels;
    
    private Long totalQuota;
    
    private Long usedQuota;
}
