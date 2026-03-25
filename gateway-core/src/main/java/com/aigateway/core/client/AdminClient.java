package com.aigateway.core.client;

import com.aigateway.common.dto.UsageLogDTO;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.common.result.Result;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * gateway-core 调用 gateway-admin 的内部客户端
 *
 * 缓存策略：API Key 验证结果本地缓存 60 秒（可配置），
 * 减少对 admin 服务的同步 HTTP 调用，提升网关吞吐量。
 * admin 服务不可用时仍可使用缓存结果，降低故障影响范围。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminClient {

    @Value("${admin.base-url:http://localhost:9082}")
    private String adminBaseUrl;

    /** 内部接口共享密钥，admin 侧会校验此 Header */
    @Value("${admin.internal-secret:aigateway-internal-2026}")
    private String internalSecret;

    @Value("${admin.request-timeout-ms:8000}")
    private long adminRequestTimeoutMs;

    /** API Key 验证结果缓存时间（秒），默认 60 秒 */
    @Value("${gateway.cache.api-key-ttl-seconds:60}")
    private long apiKeyCacheTtlSeconds;

    /** API Key 缓存最大条目数 */
    @Value("${gateway.cache.api-key-max-size:10000}")
    private long apiKeyCacheMaxSize;

    private final WebClient.Builder webClientBuilder;
    private volatile WebClient webClient;

    /**
     * API Key 验证结果本地缓存
     * key: raw API Key 字符串
     * value: ApiKeyInfo（null 表示验证失败，用 Optional 替代以区分"未缓存"和"缓存失败"）
     *
     * 注意：此缓存在应用启动后延迟初始化（需要读取配置值），
     * 使用 volatile + 双重检查锁保证线程安全。
     */
    private volatile Cache<String, ApiKeyInfo> apiKeyCache;

    private WebClient webClient() {
        if (webClient == null) {
            synchronized (this) {
                if (webClient == null) {
                    webClient = webClientBuilder.baseUrl(adminBaseUrl).build();
                }
            }
        }
        return webClient;
    }

    private Cache<String, ApiKeyInfo> apiKeyCache() {
        if (apiKeyCache == null) {
            synchronized (this) {
                if (apiKeyCache == null) {
                    apiKeyCache = Caffeine.newBuilder()
                            .maximumSize(apiKeyCacheMaxSize)
                            .expireAfterWrite(apiKeyCacheTtlSeconds, TimeUnit.SECONDS)
                            .recordStats()
                            .build();
                    log.info("[AdminClient] API Key cache initialized: ttl={}s, maxSize={}",
                            apiKeyCacheTtlSeconds, apiKeyCacheMaxSize);
                }
            }
        }
        return apiKeyCache;
    }

    /**
     * 验证 API Key（带本地缓存）
     * 缓存命中直接返回，减少 admin 服务调用次数。
     * 缓存仅存储验证成功的结果，失败（null）不缓存，避免短期内封禁已撤销 Key。
     */
    public ApiKeyInfo validateApiKey(String apiKey) {
        // 先查本地缓存
        ApiKeyInfo cached = apiKeyCache().getIfPresent(apiKey);
        if (cached != null) {
            log.debug("[AdminClient] API Key cache hit: keyPrefix={}",
                    apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : "***");
            return cached;
        }

        // 缓存未命中，调用 admin
        try {
            Result<ApiKeyInfo> result = webClient().get()
                    .uri(u -> u.path("/api/admin/keys/validate").queryParam("key", apiKey).build())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<ApiKeyInfo>>() {})
                    .timeout(Duration.ofMillis(adminRequestTimeoutMs))
                    .block();

            if (result == null || result.getCode() != 200 || result.getData() == null) {
                log.warn("[AdminClient] API Key validation failed: code={}",
                        result != null ? result.getCode() : null);
                return null;
            }

            ApiKeyInfo info = result.getData();
            // 仅缓存验证成功的结果
            apiKeyCache().put(apiKey, info);
            log.debug("[AdminClient] API Key validated & cached: keyId={}", info.getId());
            return info;

        } catch (Exception e) {
            log.error("[AdminClient] Validate API Key failed", e);
            return null;
        }
    }

    /**
     * 主动使缓存中的 API Key 失效（Key 被撤销时调用）
     */
    public void invalidateApiKeyCache(String apiKey) {
        apiKeyCache().invalidate(apiKey);
        log.info("[AdminClient] API Key cache invalidated: keyPrefix={}",
                apiKey.length() > 8 ? apiKey.substring(0, 8) + "..." : "***");
    }

    public void sendUsageLog(UsageLogDTO usageLog) {
        webClient().post().uri("/api/admin/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Internal-Secret", internalSecret)
                .bodyValue(usageLog)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                    v -> log.debug("[AdminClient] Usage log sent: {}", usageLog.getRequestId()),
                    e -> log.error("[AdminClient] Send usage log failed: {}", usageLog.getRequestId(), e)
                );
    }

    public List<String> listAvailableModelIds() {
        try {
            Result<List<Map<String, Object>>> result = webClient().get()
                    .uri("/api/admin/models")
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {})
                    .timeout(Duration.ofMillis(adminRequestTimeoutMs))
                    .block();
            if (result == null || result.getData() == null) return List.of();
            return result.getData().stream()
                    .filter(m -> Integer.valueOf(1).equals(m.get("status")))
                    .map(m -> String.valueOf(m.get("modelName")))
                    .filter(s -> !"null".equals(s))
                    .toList();
        } catch (Exception e) {
            log.error("[AdminClient] List available models failed", e);
            return List.of();
        }
    }

    /**
     * 拉取 admin 中启用的渠道列表（供动态路由使用）
     */
    public List<Map<String, Object>> listEnabledChannels() {
        try {
            Result<List<Map<String, Object>>> result = webClient().get()
                    .uri("/api/admin/channels/internal")
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Result<List<Map<String, Object>>>>() {})
                    .timeout(Duration.ofMillis(adminRequestTimeoutMs))
                    .block();
            if (result == null || result.getData() == null) return List.of();
            return result.getData();
        } catch (Exception e) {
            // reactor .block() 超时通常会包装为 ReactiveException，需要向下找 TimeoutException
            if (containsTimeoutException(e)) {
                log.warn("[AdminClient] List enabled channels timeout: {}", e.getMessage());
            } else {
                log.error("[AdminClient] List enabled channels failed", e);
            }
            // 返回 null 表示“调用失败”，让上层区分“失败”和“空列表”
            return null;
        }
    }

    private static boolean containsTimeoutException(Throwable e) {
        if (e == null) return false;
        if (e instanceof java.util.concurrent.TimeoutException) return true;
        Throwable t = e;
        // 递归检查 cause 链，避免只看最外层包装异常
        while (t != null && t.getCause() != null) {
            if (t.getCause() instanceof java.util.concurrent.TimeoutException) return true;
            t = t.getCause();
        }
        return false;
    }

    public void deductQuotaAsync(Long keyId, long tokens) {
        if (keyId == null || tokens <= 0) return;
        webClient().post()
                .uri(u -> u.path("/api/admin/keys/{keyId}/deduct-quota")
                        .queryParam("tokens", tokens).build(keyId))
                .header("X-Internal-Secret", internalSecret)
                .retrieve()
                .bodyToMono(Void.class)
                .subscribe(
                    v -> log.debug("[AdminClient] Quota deducted: keyId={}, tokens={}", keyId, tokens),
                    e -> log.error("[AdminClient] Deduct quota failed: keyId={}, tokens={}", keyId, tokens, e)
                );
    }
}
