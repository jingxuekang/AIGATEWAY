package com.aigateway.admin.controller;

import com.aigateway.admin.entity.Provider;
import com.aigateway.admin.service.ProviderService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Provider 管理
 *
 * 安全要点：
 *   - listAll() 返回脱敏 apiKey（前端永远看不到明文密鑰）
 *   - testConnection() 后端代理调用 Provider，前端只传测试文本
 */
@Slf4j
@Tag(name = "Provider Management", description = "Provider 管理")
@RestController
@RequestMapping("/api/admin/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private void requireAdmin(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        if (role == null || !"admin".equals(role.toString())) {
            throw new BusinessException(403, "Forbidden: admin role required");
        }
    }

    @Operation(summary = "获取 Provider 列表（apiKey 已脱敏）")
    @GetMapping
    public Result<List<Provider>> list(HttpServletRequest request) {
        requireAdmin(request);
        return Result.success(providerService.listAll());
    }

    @Operation(summary = "创建 Provider")
    @PostMapping
    public Result<Provider> create(HttpServletRequest request, @RequestBody Provider provider) {
        requireAdmin(request);
        provider.setId(null);
        provider.setCreateTime(LocalDateTime.now());
        provider.setUpdateTime(LocalDateTime.now());
        if (provider.getStatus() == null) provider.setStatus(1);
        if (provider.getDeleted() == null) provider.setDeleted(0);
        providerService.save(provider);
        provider.setApiKey(null);
        return Result.success(provider);
    }

    @Operation(summary = "更新 Provider")
    @PutMapping("/{id}")
    public Result<Boolean> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Provider provider) {
        requireAdmin(request);
        provider.setId(id);
        provider.setUpdateTime(LocalDateTime.now());
        return Result.success(providerService.updateById(provider));
    }

    @Operation(summary = "删除 Provider")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(HttpServletRequest request, @PathVariable Long id) {
        requireAdmin(request);
        Provider p = new Provider();
        p.setId(id);
        p.setDeleted(1);
        p.setUpdateTime(LocalDateTime.now());
        return Result.success(providerService.updateById(p));
    }

    /**
     * 后端代理测试 Provider 连通性
     * 安全设计：前端只传 providerId + 测试文本，apiKey 全程不离开服务器
     */
    @Operation(summary = "测试 Provider 连通性（后端代理，apiKey 不暴露前端）")
    @PostMapping("/{id}/test")
    public Result<Map<String, Object>> testConnection(
            HttpServletRequest request,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        requireAdmin(request);

        Provider provider = providerService.getWithDecryptedKey(id);
        if (provider == null || provider.getDeleted() == 1) {
            throw new BusinessException(404, "Provider not found");
        }
        if (provider.getStatus() == 0) {
            throw new BusinessException(400, "Provider is disabled");
        }

        String testContent = body.getOrDefault("content", "Hello, this is a connectivity test.");
        String model = body.getOrDefault("model", "gpt-3.5-turbo");

        try {
            Object content =
                (model != null && (model.startsWith("glm-4.6v") || model.startsWith("glm-4.6V")))
                    ? List.of(Map.of("type", "text", "text", testContent))
                    : testContent;

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", content)),
                    "max_tokens", 100
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(provider.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + provider.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            String aiText = "";
            if (resp.statusCode() == 200) {
                Map<?, ?> respMap = objectMapper.readValue(resp.body(), Map.class);
                if (respMap.containsKey("choices")) {
                    var choices = (List<?>) respMap.get("choices");
                    if (!choices.isEmpty()) {
                        var choice = (Map<?, ?>) choices.get(0);
                        var msg = (Map<?, ?>) choice.get("message");
                        if (msg != null) aiText = (String) msg.get("content");
                    }
                }
                log.info("Provider test success: provider={}, model={}", provider.getCode(), model);
                return Result.success(Map.of(
                        "success", true,
                        "provider", provider.getName(),
                        "model", model,
                        "response", aiText
                ));
            } else {
                log.warn("Provider test failed: provider={}, status={}, body={}", provider.getCode(), resp.statusCode(), resp.body());
                return Result.success(Map.of(
                        "success", false,
                        "provider", provider.getName(),
                        "error", "HTTP " + resp.statusCode() + ": " + resp.body()
                ));
            }
        } catch (Exception e) {
            log.warn("Provider test failed: provider={}, error={}", provider.getCode(), e.getMessage());
            return Result.success(Map.of(
                    "success", false,
                    "provider", provider.getName(),
                    "error", e.getMessage() != null ? e.getMessage() : "Connection failed"
            ));
        }
    }
}
