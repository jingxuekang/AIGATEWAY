package com.aigateway.core.service;

import com.aigateway.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class BudgetGuardService {

    @Value("${gateway.budget.global-daily-tokens:10000000}")
    private long globalDailyTokenLimit;

    @Value("${gateway.budget.per-key-daily-tokens:100000}")
    private long perKeyDailyTokenLimit;

    @Value("${gateway.budget.per-model-daily-tokens:5000000}")
    private long perModelDailyTokenLimit;

    @Value("${gateway.budget.enabled:true}")
    private boolean budgetEnabled;

    private final AtomicLong globalDailyUsed = new AtomicLong(0);
    private final Map<String, AtomicLong> keyDailyUsed = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> modelDailyUsed = new ConcurrentHashMap<>();
    private volatile long lastResetDay = currentDay();

    public void checkBudget(String apiKeyId, String model) {
        if (!budgetEnabled) return;
        resetIfNewDay();
        if (globalDailyTokenLimit > 0 && globalDailyUsed.get() >= globalDailyTokenLimit) {
            log.warn("[Budget] Global limit exceeded: used={}, limit={}",
                globalDailyUsed.get(), globalDailyTokenLimit);
            throw new BusinessException(429, "Global daily token budget exhausted.");
        }
        if (apiKeyId != null && perKeyDailyTokenLimit > 0) {
            long used = keyDailyUsed.computeIfAbsent(apiKeyId, k -> new AtomicLong(0)).get();
            if (used >= perKeyDailyTokenLimit) {
                log.warn("[Budget] Per-key limit exceeded: keyId={}, used={}", apiKeyId, used);
                throw new BusinessException(429, "Daily token budget exhausted for this API key.");
            }
        }
        if (model != null && perModelDailyTokenLimit > 0) {
            long used = modelDailyUsed.computeIfAbsent(model, k -> new AtomicLong(0)).get();
            if (used >= perModelDailyTokenLimit) {
                log.warn("[Budget] Per-model limit exceeded: model={}, used={}", model, used);
                throw new BusinessException(429, "Daily token budget exhausted for model: " + model);
            }
        }
    }

    public void recordUsage(String apiKeyId, String model, int tokensUsed) {
        if (!budgetEnabled || tokensUsed <= 0) return;
        resetIfNewDay();
        globalDailyUsed.addAndGet(tokensUsed);
        if (apiKeyId != null)
            keyDailyUsed.computeIfAbsent(apiKeyId, k -> new AtomicLong(0)).addAndGet(tokensUsed);
        if (model != null)
            modelDailyUsed.computeIfAbsent(model, k -> new AtomicLong(0)).addAndGet(tokensUsed);
    }

    public Map<String, Object> getUsageSummary() {
        return Map.of(
            "globalDailyUsed", globalDailyUsed.get(),
            "globalDailyLimit", globalDailyTokenLimit,
            "keyCount", keyDailyUsed.size(),
            "modelCount", modelDailyUsed.size()
        );
    }

    private void resetIfNewDay() {
        long today = currentDay();
        if (today != lastResetDay) {
            synchronized (this) {
                if (today != lastResetDay) {
                    globalDailyUsed.set(0);
                    keyDailyUsed.clear();
                    modelDailyUsed.clear();
                    lastResetDay = today;
                }
            }
        }
    }

    private long currentDay() {
        // 使用系统默认时区（北京时间）计算当天，避免 UTC 00:00 触发重置
        return java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toEpochDay();
    }
}
