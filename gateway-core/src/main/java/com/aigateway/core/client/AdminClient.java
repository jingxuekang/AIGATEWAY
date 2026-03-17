package com.aigateway.core.client;

import com.aigateway.common.dto.UsageLogDTO;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * 调用管理端网关的客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    @Value("${admin.base-url:http://localhost:9082}")
    private String adminBaseUrl;

    private final WebClient.Builder webClientBuilder;

    private WebClient webClient() {
        return webClientBuilder
                .baseUrl(adminBaseUrl)
                .build();
    }

    /**
     * 验证 API Key
     */
    public ApiKeyInfo validateApiKey(String apiKey) {
        try {
            Result<ApiKeyInfo> result = webClient()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/admin/keys/validate")
                            .queryParam("key", apiKey)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<ApiKeyInfo>>() {})
                    .block();
            if (result == null || result.getCode() != 200 || result.getData() == null) {
                log.warn("API Key validation failed, code={}, message={}",
                        result != null ? result.getCode() : null,
                        result != null ? result.getMessage() : null);
                return null;
            }
            return result.getData();
        } catch (Exception e) {
            log.error("Validate API Key failed", e);
            return null;
        }
    }

    /**
     * 发送 Usage 日志到管理端（异步，admin 不可用时不影响主流程）
     */
    public void sendUsageLog(UsageLogDTO usageLog) {
        webClient()
                .post()
                .uri("/api/admin/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(usageLog)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        v -> log.debug("Usage log sent: requestId={}", usageLog.getRequestId()),
                        e -> log.error("Send usage log failed: requestId={}", usageLog.getRequestId(), e)
                );
    }

    /**
     * 获取可用模型 ID 列表（供 /v1/models 接口使用）
     */
    public List<String> listAvailableModelIds() {
        try {
            Result<List<Map<String, Object>>> result = webClient()
                    .get()
                    .uri("/api/admin/models")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {})
                    .block();
            if (result == null || result.getData() == null) return List.of();
            return result.getData().stream()
                    .filter(m -> Integer.valueOf(1).equals(m.get("status")))
                    .map(m -> String.valueOf(m.get("modelName")))
                    .filter(s -> !"null".equals(s))
                    .toList();
        } catch (Exception e) {
            log.error("List available models failed", e);
            return List.of();
        }
    }

    /**
     * 异步扣减 API Key 配额（fire-and-forget）
     */
    public void deductQuotaAsync(Long keyId, long tokens) {
        if (keyId == null || tokens <= 0) return;
        webClient()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/admin/keys/{keyId}/deduct-quota")
                        .queryParam("tokens", tokens)
                        .build(keyId))
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                        v -> log.debug("Quota deducted: keyId={}, tokens={}", keyId, tokens),
                        e -> log.error("Deduct quota failed: keyId={}, tokens={}", keyId, tokens, e)
                );
    }
}
