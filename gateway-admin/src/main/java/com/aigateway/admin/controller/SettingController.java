package com.aigateway.admin.controller;

import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "Setting Management", description = "系统设置")
@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
public class SettingController {
    
    @Operation(summary = "获取系统设置")
    @GetMapping
    public Result<Map<String, Object>> getSettings() {
        Map<String, Object> settings = new HashMap<>();
        
        Map<String, Object> system = new HashMap<>();
        system.put("siteName", "AI Model Gateway");
        system.put("siteUrl", "https://gateway.example.com");
        system.put("defaultQuota", 100000);
        system.put("enableRegistration", true);
        system.put("enableApiKeyApplication", true);
        settings.put("system", system);
        
        Map<String, Object> security = new HashMap<>();
        security.put("maxRequestsPerMinute", 100);
        security.put("maxRequestsPerDay", 10000);
        security.put("enableIpWhitelist", false);
        security.put("ipWhitelist", "");
        security.put("enableAuditLog", true);
        settings.put("security", security);
        
        return Result.success(settings);
    }
    
    @Operation(summary = "更新系统设置")
    @PutMapping
    public Result<Boolean> updateSettings(@RequestBody Map<String, Object> settings) {
        return Result.success(true);
    }
}
