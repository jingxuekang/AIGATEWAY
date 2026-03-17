package com.aigateway.core.service;

import com.aigateway.common.context.ApiKeyContext;
import com.aigateway.common.dto.UsageLogDTO;
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

import java.time.LocalDateTime;
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

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("=== Registered Model Providers ===");
        providers.forEach(p -> log.info("  Provider: {} - {}", p.getProviderName(), p.getClass().getSimpleName()));
        log.info("=== Total {} providers ===", providers.size());
    }

    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        ModelProvider provider = findProvider(request.getModel());
        checkModelPermission(request.getModel());
        try {
            ChatResponse response = provider.chat(request);
            long latency = System.currentTimeMillis() - startTime;
            usageLogService.logUsage(request, response, latency,
                    provider.getProviderName(), "success", requestId, null, null, latency, 0L);
            deductQuotaIfNeeded(response);
            return response;
        } catch (BusinessException e) {
            long latency = System.currentTimeMillis() - startTime;
            usageLogService.logUsage(request, null, latency,
                    provider.getProviderName(), "error", requestId,
                    String.valueOf(e.getCode()), e.getMessage(), latency, 0L);
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            usageLogService.logUsage(request, null, latency,
                    provider.getProviderName(), "error", requestId, null, e.getMessage(), latency, 0L);
            throw e;
        }
    }

    /**
     * 流式响应，同时采集 TTFT（首 token 延迟）和 TPOT（平均每 token 时间）
     */
    public Flux<String> chatStream(ChatRequest request) {
        checkModelPermission(request.getModel());
        ModelProvider provider = findProvider(request.getModel());
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        AtomicLong ttft = new AtomicLong(-1);
        AtomicInteger tokenCount = new AtomicInteger(0);
        AtomicLong firstTokenTime = new AtomicLong(0);

        return provider.chatStream(request)
                .doOnNext(chunk -> {
                    if (chunk.contains("\"content\":") && !chunk.contains("[DONE]")) {
                        long now = System.currentTimeMillis();
                        if (ttft.get() == -1) {
                            ttft.set(now - startTime);
                            firstTokenTime.set(now);
                        }
                        tokenCount.incrementAndGet();
                    }
                })
                .doOnComplete(() -> {
                    long totalLatency = System.currentTimeMillis() - startTime;
                    long ttftMs = ttft.get() > 0 ? ttft.get() : totalLatency;
                    int tokens = tokenCount.get();
                    long tpotMs = tokens > 0
                            ? (System.currentTimeMillis() - firstTokenTime.get()) / Math.max(tokens, 1)
                            : 0L;
                    usageLogService.logUsage(request, null, totalLatency,
                            provider.getProviderName(), "success", requestId,
                            null, null, ttftMs, tpotMs);
                })
                .doOnError(e -> {
                    long latency = System.currentTimeMillis() - startTime;
                    usageLogService.logUsage(request, null, latency,
                            provider.getProviderName(), "error", requestId,
                            null, e.getMessage(), latency, 0L);
                });
    }

    private ModelProvider findProvider(String model) {
        return providers.stream()
                .filter(p -> p.supports(model))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Unsupported model: " + model));
    }

    private void checkModelPermission(String model) {
        ApiKeyInfo keyInfo = ApiKeyContext.get();
        if (keyInfo == null) return;
        List<String> allowed = keyInfo.getAllowedModels();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(model)) {
            throw new BusinessException("Model not allowed for this API key: " + model);
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
