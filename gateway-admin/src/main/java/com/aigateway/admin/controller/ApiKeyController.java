package com.aigateway.admin.controller;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.service.ApiKeyService;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Key 控制器
 */
@Tag(name = "API Key Management", description = "API Key 管理")
@RestController
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    
    @Operation(summary = "创建 API Key")
    @PostMapping
    public Result<ApiKey> createKey(@RequestBody ApiKey apiKey) {
        ApiKey created = apiKeyService.createKey(apiKey);
        return Result.success(created);
    }
    
    @Operation(summary = "获取用户 API Key 列表")
    @GetMapping("/user/{userId}")
    public Result<List<ApiKey>> listUserKeys(@PathVariable Long userId) {
        List<ApiKey> keys = apiKeyService.listByUser(userId);
        return Result.success(keys);
    }
    
    @Operation(summary = "吊销 API Key")
    @PutMapping("/{keyId}/revoke")
    public Result<Boolean> revokeKey(@PathVariable Long keyId) {
        boolean success = apiKeyService.revokeKey(keyId);
        return Result.success(success);
    }

    @Operation(summary = "验证 API Key")
    @GetMapping("/validate")
    public Result<ApiKeyInfo> validateKey(@RequestParam("key") String keyValue) {
        ApiKey apiKey = apiKeyService.validateKey(keyValue);
        if (apiKey == null) {
            return Result.error(401, "Invalid API Key");
        }
        ApiKeyInfo info = new ApiKeyInfo();
        info.setId(apiKey.getId());
        info.setUserId(apiKey.getUserId());
        info.setTenantId(apiKey.getTenantId());
        info.setAppId(apiKey.getAppId());
        if (apiKey.getAllowedModels() != null && !apiKey.getAllowedModels().isEmpty()) {
            info.setAllowedModels(java.util.Arrays.asList(apiKey.getAllowedModels().split(",")));
        }
        return Result.success(info);
    }

    @Operation(summary = "扣减配额", description = "由网关调用，扣减 API Key 的 token 配额")
    @PostMapping("/{keyId}/deduct-quota")
    public Result<Boolean> deductQuota(@PathVariable Long keyId,
                                        @RequestParam long tokens) {
        boolean success = apiKeyService.deductQuota(keyId, tokens);
        return Result.success(success);
    }
}