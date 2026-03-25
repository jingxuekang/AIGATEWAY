package com.aigateway.core.service;

import com.aigateway.common.context.ApiKeyContext;
import com.aigateway.common.exception.BusinessException;
import com.aigateway.common.model.ApiKeyInfo;
import com.aigateway.core.client.AdminClient;
import com.aigateway.core.metrics.LlmMetrics;
import com.aigateway.provider.adapter.ModelProvider;
import com.aigateway.provider.model.ChatRequest;
import com.aigateway.provider.model.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final List<ModelProvider> providers;
    private final UsageLogService usageLogService;
    private final AdminClient adminClient;
    private final LlmMetrics llmMetrics;
    private final CircuitBreakerService circuitBreakerService;
    private final BudgetGuardService budgetGuardService;
    private final PromptGuardService promptGuardService;
    private final SemanticCacheService semanticCacheService;
    private final SmartRoutingService smartRoutingService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=== Registered Model Providers ===");
        providers.forEach(p -> log.info("  Provider: {} - {}", p.getProviderName(), p.getClass().getSimpleName()));
        log.info("=== Total {} providers ===", providers.size());
    }

    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // 1. 提示词注入检测
        promptGuardService.check(request);

        // 2. 模型权限检查
        checkModelPermission(request.getModel());

        // 3. 预算检查
        String apiKeyId = getApiKeyId();
        budgetGuardService.checkBudget(apiKeyId, request.getModel());

        // 4. 语义缓存命中
        ChatResponse cached = semanticCacheService.get(request);
        if (cached != null) {
            log.info("[Cache] Returning cached response for model={}", request.getModel());
            return cached;
        }

        // 5. 智能路由选择 Provider
        ModelProvider provider = smartRoutingService.selectProvider(request.getModel());

        try {
            // 6. 熔断器包装调用
            ChatResponse response = circuitBreakerService.executeWithCircuitBreaker(
                provider.getProviderName(), () -> provider.chat(request));

            long latency = System.currentTimeMillis() - startTime;
            smartRoutingService.recordSuccess(provider.getProviderName());

            // 7. 记录预算消耗
            if (response.getUsage() != null && response.getUsage().getTotalTokens() != null) {
                budgetGuardService.recordUsage(apiKeyId, request.getModel(),
                    response.getUsage().getTotalTokens());
            }

            // 8. 写入缓存
            semanticCacheService.put(request, response);

            // 9. 日志上报
            usageLogService.logUsage(request, response, UsageLogContext.builder()
                .requestId(requestId)
                .providerName(provider.getProviderName())
                .status("success")
                .latencyMs(latency)
                .ttftMs(latency)
                .tpotMs(0L)
                .build());

            // 10. 配额扣减
            deductQuotaIfNeeded(response);
            return response;

        } catch (BusinessException e) {
            long latency = System.currentTimeMillis() - startTime;
            smartRoutingService.recordFailure(provider.getProviderName());
            usageLogService.logUsage(request, null, UsageLogContext.builder()
                .requestId(requestId)
                .providerName(provider.getProviderName())
                .status("error")
                .errorCode(String.valueOf(e.getCode()))
                .errorMessage(e.getMessage())
                .latencyMs(latency)
                .ttftMs(latency)
                .tpotMs(0L)
                .build());
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            smartRoutingService.recordFailure(provider.getProviderName());
            usageLogService.logUsage(request, null, UsageLogContext.builder()
                .requestId(requestId)
                .providerName(provider.getProviderName())
                .status("error")
                .errorMessage(e.getMessage())
                .latencyMs(latency)
                .ttftMs(latency)
                .tpotMs(0L)
                .build());
            throw e;
        }
    }

    public Flux<String> chatStream(ChatRequest request) {
        // 1. 提示词注入检测
        promptGuardService.check(request);

        // 2. 模型权限检查
        checkModelPermission(request.getModel());

        // 3. 预算检查
        String apiKeyId = getApiKeyId();
        budgetGuardService.checkBudget(apiKeyId, request.getModel());

        // 4. 智能路由
        ModelProvider provider = smartRoutingService.selectProvider(request.getModel());
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        AtomicLong ttft = new AtomicLong(-1);
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicLong firstTokenTime = new AtomicLong(0);

        return circuitBreakerService.executeStreamWithCircuitBreaker(
                provider.getProviderName(), () -> provider.chatStream(request))
                .doOnNext(chunk -> {
                    // 记录首个有内容的 chunk 时间（用于 TTFT 计算）
                    if (ttft.get() == -1 && chunk.contains("\"content\":") && !chunk.contains("[DONE]")) {
                        long now = System.currentTimeMillis();
                        if (ttft.compareAndSet(-1, now - startTime)) {
                            firstTokenTime.set(now);
                        }
                    }
                    // 从包含 usage 的最终 chunk 中提取精确 token 数
                    if (chunk.contains("\"usage\":") && chunk.contains("\"total_tokens\":")) {
                        try {
                            int idx = chunk.indexOf("\"total_tokens\":");
                            if (idx >= 0) {
                                int start = idx + 15;
                                int end = start;
                                while (end < chunk.length() && (Character.isDigit(chunk.charAt(end)))) end++;
                                int total = Integer.parseInt(chunk.substring(start, end).trim());
                                if (total > 0) tokenCount.set(total);
                            }
                        } catch (Exception ignored) {}
                    }
                })
                .doOnComplete(() -> {
                    long totalLatency = System.currentTimeMillis() - startTime;
                    long ttftMs = ttft.get() > 0 ? ttft.get() : totalLatency;
                    int tokens = tokenCount.get();
                    long tpotMs = tokens > 1
                        ? (System.currentTimeMillis() - firstTokenTime.get()) / (tokens - 1)
                        : 0L;
                    smartRoutingService.recordSuccess(provider.getProviderName());
                    budgetGuardService.recordUsage(apiKeyId, request.getModel(), tokens);
                    usageLogService.logUsage(request, null, UsageLogContext.builder()
                        .requestId(requestId)
                        .providerName(provider.getProviderName())
                        .status("success")
                        .latencyMs(totalLatency)
                        .ttftMs(ttftMs)
                        .tpotMs(tpotMs)
                        .build());
                })
                .doOnError(e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    smartRoutingService.recordFailure(provider.getProviderName());
                    usageLogService.logUsage(request, null, UsageLogContext.builder()
                        .requestId(requestId)
                        .providerName(provider.getProviderName())
                        .status("error")
                        .errorMessage(e.getMessage())
                        .latencyMs(latency)
                        .ttftMs(latency)
                        .tpotMs(0L)
                        .build());
                });
    }

    /**
     * 多模态请求 — 通过 Spring AI 标准路由，不再依赖特定 Provider 实现
     * 将多模态请求路由到支持该模型的 Provider 的标准 chat() 方法
     */
    public ChatResponse chatMultiModal(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // 打印请求体用于调试
        try {
            String reqJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request);
            if (reqJson.length() > 500) reqJson = reqJson.substring(0, 500) + "...[truncated]";
            log.debug("[chatMultiModal] request={}", reqJson);
        } catch (Exception ignored) {}

        String apiKeyId = getApiKeyId();
        budgetGuardService.checkBudget(apiKeyId, request.getModel());

        // 通过智能路由找到支持该模型的 Provider（如 volcano）
        ModelProvider provider = smartRoutingService.selectProvider(request.getModel());

        try {
            ChatResponse response = circuitBreakerService.executeWithCircuitBreaker(
                provider.getProviderName(), () -> provider.chat(request));

            long latency = System.currentTimeMillis() - startTime;
            smartRoutingService.recordSuccess(provider.getProviderName());

            if (response.getUsage() != null && response.getUsage().getTotalTokens() != null) {
                budgetGuardService.recordUsage(apiKeyId, request.getModel(),
                    response.getUsage().getTotalTokens());
            }

            usageLogService.logUsage(request, response, UsageLogContext.builder()
                .requestId(requestId)
                .providerName(provider.getProviderName())
                .status("success")
                .latencyMs(latency)
                .ttftMs(latency)
                .tpotMs(0L)
                .build());

            deductQuotaIfNeeded(response);
            return response;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            smartRoutingService.recordFailure(provider.getProviderName());
            usageLogService.logUsage(request, null, UsageLogContext.builder()
                .requestId(requestId)
                .providerName(provider.getProviderName())
                .status("error")
                .errorMessage(e.getMessage())
                .latencyMs(latency)
                .ttftMs(latency)
                .tpotMs(0L)
                .build());
            throw e;
        }
    }

    private String getApiKeyId() {
        ApiKeyInfo keyInfo = ApiKeyContext.get();
        return keyInfo != null && keyInfo.getId() != null ? keyInfo.getId().toString() : null;
    }

    private void checkModelPermission(String model) {
        ApiKeyInfo keyInfo = ApiKeyContext.get();
        if (keyInfo == null) return;
        List<String> allowed = keyInfo.getAllowedModels();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(model)) {
            throw new BusinessException(403, "Model not allowed for this API key: " + model);
        }
    }

    private void deductQuotaIfNeeded(ChatResponse response) {
        if (response == null || response.getUsage() == null) return;
        Integer total = response.getUsage().getTotalTokens();
        if (total == null || total <= 0) return;
        ApiKeyInfo keyInfo = ApiKeyContext.get();
        if (keyInfo == null || keyInfo.getId() == null) return;
        adminClient.deductQuotaAsync(keyInfo.getId(), total);
    }
}
