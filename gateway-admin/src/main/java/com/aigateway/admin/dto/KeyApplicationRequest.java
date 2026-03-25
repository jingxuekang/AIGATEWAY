package com.aigateway.admin.dto;

import lombok.Data;
import java.util.List;

/**
 * API Key 申请请求 DTO
 * allowedModels 前端传数组，后端转为逗号分隔字符串存储
 */
@Data
public class KeyApplicationRequest {
    private String keyName;
    private List<String> allowedModels;
    private String reason;
    private String tenantId;
    private String appId;

    /**
     * 将 allowedModels 列表转为逗号分隔字符串
     */
    public String getAllowedModelsAsString() {
        if (allowedModels == null || allowedModels.isEmpty()) return null;
        return String.join(",", allowedModels);
    }
}
