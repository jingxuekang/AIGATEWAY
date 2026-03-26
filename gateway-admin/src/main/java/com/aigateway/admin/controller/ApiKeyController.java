package com.aigateway.admin.controller;

import com.aigateway.admin.entity.ApiKey;
import com.aigateway.admin.entity.User;
import com.aigateway.admin.entity.Model;
import com.aigateway.admin.service.ApiKeyService;
import com.aigateway.admin.service.ModelService;
import com.aigateway.admin.service.ModelSubscriptionService;
import com.aigateway.admin.service.UserService;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "API Key Management", description = "API Key \u7ba1\u7406")
@RestController
@RequestMapping("/api/admin/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final UserService userService;
    private final ModelService modelService;
    private final ModelSubscriptionService modelSubscriptionService;

    @Value("${gateway.base-url:http://localhost:9080}")
    private String gatewayBaseUrl;

    private static String getRole(HttpServletRequest request) {
        Object role = request.getAttribute("role");
        return role != null ? role.toString() : "user";
    }

    private static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "Not authenticated");
        return (Long) userId;
    }

    private static boolean isAdmin(HttpServletRequest request) {
        return "admin".equals(getRole(request));
    }

    private static void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) throw new BusinessException(403, "Admin role required");
    }

    @Operation(summary = "合并 allowedModels 到已有 Key（仅 admin）")
    @PostMapping("/{keyId}/merge-allowed-models")
    public Result<ApiKey> mergeAllowedModels(@PathVariable Long keyId,
                                              @RequestBody Map<String, Object> body,
                                              HttpServletRequest request) {
        requireAdmin(request);
        ApiKey key = apiKeyService.getById(keyId);
        if (key == null) throw new BusinessException(404, "Key not found");

        Object newAllowedObj = body.get("newAllowedModels");
        if (newAllowedObj == null) throw new BusinessException(400, "newAllowedModels is required");

        java.util.List<String> newAllowedModels = new java.util.ArrayList<>();
        if (newAllowedObj instanceof java.util.List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) newAllowedModels.add(s);
                }
            }
        } else if (newAllowedObj instanceof String s) {
            for (String t : s.split(",")) {
                String tt = t.trim();
                if (!tt.isEmpty()) newAllowedModels.add(tt);
            }
        } else {
            throw new BusinessException(400, "newAllowedModels must be an array or comma-separated string");
        }

        if (newAllowedModels.isEmpty()) return Result.success(key);

        String existingAllowed = key.getAllowedModels();
        java.util.Set<String> merged = new java.util.LinkedHashSet<>();
        if (existingAllowed != null && !existingAllowed.isBlank()) {
            for (String s : existingAllowed.split(",")) {
                String t = s.trim();
                if (!t.isEmpty()) merged.add(t);
            }
        }

        java.util.List<String> actuallyAdded = new java.util.ArrayList<>();
        for (String modelName : newAllowedModels) {
            if (merged.add(modelName)) actuallyAdded.add(modelName);
        }

        if (!actuallyAdded.isEmpty()) {
            key.setAllowedModels(String.join(",", merged));
            key.setUpdateTime(java.time.LocalDateTime.now());
            apiKeyService.updateById(key);

            // 同步订阅新增的模型
            String tenantId = key.getTenantId() != null ? key.getTenantId() : "";
            String appId = key.getAppId() != null ? key.getAppId() : "";
            for (String modelName : actuallyAdded) {
                Model model = modelService.getByName(modelName);
                if (model != null) {
                    modelSubscriptionService.subscribe(model.getId(), key.getUserId(), tenantId, appId);
                }
            }
        }

        // 避免把真实 keyValue 返回给前端
        key.setKeyValue(null);
        return Result.success(key);
    }

    /** \u8111\u5145 username \u5b57\u6bb5 */
    private void fillUsernames(List\u003cApiKey\u003e keys) {
        if (keys == null || keys.isEmpty()) return;
        List\u003cLong\u003e ids = keys.stream().map(ApiKey::getUserId).filter(id -> id != null).distinct().toList();
        if (ids.isEmpty()) return;
        Map\u003cLong, String\u003e userMap = userService.listByIds(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        keys.forEach(k -> k.setUsername(userMap.getOrDefault(k.getUserId(), String.valueOf(k.getUserId()))));
    }

    /** \u8111\u654f keyValue */
    private static void maskKeys(List\u003cApiKey\u003e keys) {
        keys.forEach(k -> {
            if (k.getKeyValue() != null && k.getKeyValue().length() > 8) {
                String v = k.getKeyValue();
                k.setKeyValue(v.substring(0, 8) + "****" + v.substring(v.length() - 4));
            }
        });
    }

    @Operation(summary = "\u76f4\u63a5\u521b\u5efa API Key\uff08\u4ec5 admin\uff09")
    @PostMapping
    public Result\u003cApiKey\u003e createKey(@RequestBody ApiKey apiKey, HttpServletRequest request) {
        requireAdmin(request);
        ApiKey created = apiKeyService.createKey(apiKey);
        return Result.success(created); // \u521b\u5efa\u65f6\u8fd4\u56de\u5b8c\u6574 key
    }

    @Operation(summary = "\u67e5\u8be2\u6240\u6709 Key\uff08\u4ec5 admin\uff0c\u5e26\u7528\u6237\u540d\uff09")
    @GetMapping
    public Result\u003cList\u003cApiKey\u003e\u003e listAllKeys(HttpServletRequest request) {
        requireAdmin(request);
        List\u003cApiKey\u003e keys = apiKeyService.list();
        fillUsernames(keys);
        maskKeys(keys);
        return Result.success(keys);
    }

    @Operation(summary = "\u83b7\u53d6\u7528\u6237 API Key \u5217\u8868")
    @GetMapping("/user/{userId}")
    public Result\u003cList\u003cApiKey\u003e\u003e listUserKeys(@PathVariable Long userId, HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        if (!isAdmin(request) && !currentUserId.equals(userId))
            throw new BusinessException(403, "Access denied");
        List\u003cApiKey\u003e keys = apiKeyService.listByUser(userId);
        maskKeys(keys);
        return Result.success(keys);
    }

    @Operation(summary = "获取我的 API Key 列表")
    @GetMapping("/my")
    public Result<List<ApiKey>> myKeys(HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        List<ApiKey> keys = apiKeyService.listByUser(currentUserId);
        // 本地测试阶段，普通用户可见完整 keyValue
        return Result.success(keys);
    }

    @Operation(summary = "修改 API Key 名称（用户改自己的，admin 可改任意）")
    @PatchMapping("/{keyId}/key-name")
    public Result<Boolean> renameKey(@PathVariable Long keyId,
                                     @RequestBody Map<String, String> body,
                                     HttpServletRequest request) {
        String keyName = body != null ? body.get("keyName") : null;
        apiKeyService.updateKeyName(keyId, keyName, getUserId(request), isAdmin(request));
        return Result.success(true);
    }

    @Operation(summary = "删除 API Key（逻辑删除）")
    @DeleteMapping("/{keyId}")
    public Result<Boolean> deleteKey(@PathVariable Long keyId, HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        ApiKey key = apiKeyService.getById(keyId);
        if (key == null) throw new BusinessException(404, "Key not found");
        if (!isAdmin(request) && !currentUserId.equals(key.getUserId()))
            throw new BusinessException(403, "Access denied");
        return Result.success(apiKeyService.removeById(keyId));
    }

    @Operation(summary = "\u5410\u9500 API Key")
    @PutMapping("/{keyId}/revoke")
    public Result\u003cBoolean\u003e revokeKey(@PathVariable Long keyId, HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        if (!isAdmin(request)) {
            ApiKey key = apiKeyService.getById(keyId);
            if (key == null) throw new BusinessException(404, "Key not found");
            if (!currentUserId.equals(key.getUserId()))
                throw new BusinessException(403, "Access denied");
        }
        return Result.success(apiKeyService.revokeKey(keyId));
    }

    @Operation(summary = "用户 Playground 对话（后端代理，不暴露真实 Key）")
    @PostMapping("/{keyId}/chat")
    public Result<Map<String, Object>> userChat(@PathVariable Long keyId,
                                               @RequestBody Map<String, Object> body,
                                               HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        ApiKey key = apiKeyService.getById(keyId);
        if (key == null) throw new BusinessException(404, "Key not found");
        if (!isAdmin(request) && !currentUserId.equals(key.getUserId()))
            throw new BusinessException(403, "Access denied");
        if (key.getStatus() == null || key.getStatus() != 1)
            throw new BusinessException(400, "Key is not active");
        String model = (String) body.get("model");
        Object messages = body.get("messages");
        if (model == null || messages == null)
            throw new BusinessException(400, "model and messages are required");
        assertModelAllowedForKey(key, model);
        try {
            boolean isVolcanoMultiModal = model != null && (
                    model.startsWith("doubao-") ||
                            model.startsWith("ep-") ||
                            model.startsWith("volcano-")
            );
            // 对于豆包/火山多模态，走 gateway-core 的 /v1/responses（避免 prompt-guard/缓存等流程）
            String uri = isVolcanoMultiModal ? "/v1/responses" : "/v1/chat/completions";
            String reqBody = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(isVolcanoMultiModal
                            ? Map.of("model", model, "messages", messages)
                            : Map.of("model", model, "messages", messages));
            String resp = WebClient.builder()
                    .baseUrl(gatewayBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + key.getKeyValue())
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post().uri(uri)
                    .bodyValue(reqBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(60));
            @SuppressWarnings("unchecked")
            Map<String, Object> respMap = new com.fasterxml.jackson.databind.ObjectMapper().readValue(resp, Map.class);
            return Result.success(respMap);
        } catch (WebClientResponseException e) {
            // 解析 gateway-core 返回的错误信息，避免暴露内部 URL
            String respBody = e.getResponseBodyAsString();
            String userMsg;
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> errMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(respBody, Map.class);
                Object msg = errMap.get("message");
                userMsg = msg != null ? msg.toString() : respBody;
            } catch (Exception parseEx) {
                userMsg = respBody != null && !respBody.isBlank() ? respBody : "Request failed";
            }
            throw new BusinessException(e.getStatusCode().value(), userMsg);
        } catch (Exception e) {
            throw new BusinessException(502, "Chat request failed, please retry.");
        }
    }

    @Operation(summary = "连通性测试（后端代理）")
    @PostMapping("/{keyId}/test")
    public Result\u003cMap\u003cString, Object\u003e\u003e testKey(@PathVariable Long keyId,
                                               @RequestBody Map\u003cString, String\u003e body,
                                               HttpServletRequest request) {
        Long currentUserId = getUserId(request);
        ApiKey key = apiKeyService.getById(keyId);
        if (key == null) throw new BusinessException(404, "Key not found");
        if (!isAdmin(request) && !currentUserId.equals(key.getUserId()))
            throw new BusinessException(403, "Access denied");
        if (key.getStatus() == null || key.getStatus() != 1)
            throw new BusinessException(400, "Key is not active");
        String model = body.get("model");
        if (model == null || model.isBlank()) {
            if (key.getAllowedModels() != null && !key.getAllowedModels().isBlank()) {
                model = Arrays.stream(key.getAllowedModels().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).findFirst().orElse(null);
                if (model == null) throw new BusinessException(400, "Key allowed_models is empty");
            } else {
                model = "gpt-4o-mini";
            }
        }
        assertModelAllowedForKey(key, model);
        long start = System.currentTimeMillis();
        try {
            WebClient.builder()
                    .baseUrl(gatewayBaseUrl)
                    .defaultHeader("Authorization", "Bearer " + key.getKeyValue())
                    .defaultHeader("Content-Type", "application/json")
                    .build()
                    .post().uri("/v1/chat/completions")
                    .bodyValue(String.format(
                            "{\"model\":\"%s\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"max_tokens\":5}",
                            model))
                    .retrieve().bodyToMono(String.class).block(Duration.ofSeconds(15));
            return Result.success(Map.of("success", true, "latencyMs", System.currentTimeMillis() - start, "model", model));
        } catch (Exception e) {
            return Result.success(Map.of("success", false, "latencyMs", System.currentTimeMillis() - start, "error", e.getMessage()));
        }
    }

    @Operation(summary = "\u9a8c\u8bc1 API Key\uff08\u5185\u90e8\u63a5\u53e3\uff09")
    @GetMapping("/validate")
    public Result\u003cApiKeyInfo\u003e validateKey(@RequestParam("key") String keyValue) {
        ApiKey apiKey = apiKeyService.validateKey(keyValue);
        if (apiKey == null) return Result.error(401, "Invalid API Key");
        ApiKeyInfo info = new ApiKeyInfo();
        info.setId(apiKey.getId());
        info.setUserId(apiKey.getUserId());
        info.setTenantId(apiKey.getTenantId());
        info.setAppId(apiKey.getAppId());
        info.setStatus(apiKey.getStatus());
        info.setExpireTime(apiKey.getExpireTime());
        info.setTotalQuota(apiKey.getTotalQuota());
        info.setUsedQuota(apiKey.getUsedQuota());
        if (apiKey.getAllowedModels() != null && !apiKey.getAllowedModels().isEmpty())
            info.setAllowedModels(Arrays.asList(apiKey.getAllowedModels().split(",")));
        return Result.success(info);
    }

    @Operation(summary = "\u53e3\u51cf\u914d\u989d\uff08\u5185\u90e8\u63a5\u53e3\uff09")
    @PostMapping("/{keyId}/deduct-quota")
    public Result\u003cBoolean\u003e deductQuota(@PathVariable Long keyId, @RequestParam long tokens) {
        return Result.success(apiKeyService.deductQuota(keyId, tokens));
    }

    /** 与 gateway-core ChatService.checkModelPermission 一致：allowed_models 非空则模型必须在列表中 */
    private static void assertModelAllowedForKey(ApiKey key, String model) {
        if (key.getAllowedModels() == null || key.getAllowedModels().isBlank()) return;
        List<String> allowed = Arrays.stream(key.getAllowedModels().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        boolean ok = allowed.stream().anyMatch(a -> a.equals(model));
        if (!ok) {
            throw new BusinessException(403, "Model not allowed for this API key: " + model
                    + ". Allowed: " + String.join(", ", allowed));
        }
    }
}
