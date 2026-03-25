package com.aigateway.core.service;

import com.aigateway.common.exception.BusinessException;
import com.aigateway.provider.adapter.ModelProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能路由服务
 * - 动态渠道（channel 表配置）优先，按权重加权随机选择
 * - 无动态渠道时回退到静态 Provider（application.yml 配置）
 * - 基于 CircuitBreaker 状态做健康判断
 *
 * 重构后：dynamicProviders 类型从 DynamicChannelProvider 改为 ModelProvider，
 * 通过 ModelProvider.getWeight() 默认接口方法获取权重，彻底解耦具体实现类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartRoutingService {

    /** 静态 Provider（application.yml 配置，Spring Bean 注入） */
    private final List<ModelProvider> providers;
    private final CircuitBreakerService circuitBreakerService;

    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /** 动态渠道列表，由 ChannelProviderRegistry 定期刷新 */
    private final CopyOnWriteArrayList<ModelProvider> dynamicProviders = new CopyOnWriteArrayList<>();

    /**
     * 由 ChannelProviderRegistry 调用，刷新动态渠道列表。
     * 接受 ModelProvider 接口列表，与具体实现类解耦。
     */
    public void updateDynamicProviders(List<ModelProvider> newProviders) {
        dynamicProviders.clear();
        dynamicProviders.addAll(newProviders);
        log.info("[Router] Dynamic providers updated: count={}", newProviders.size());
    }

    public ModelProvider selectProvider(String model) {
        // 1. 优先匹配动态渠道（channel 表配置），按权重加权随机选择
        List<ModelProvider> dynCandidates = dynamicProviders.stream()
                .filter(p -> p.supports(model))
                .filter(p -> circuitBreakerService.getState(p.getProviderName()) != CircuitBreaker.State.OPEN)
                .toList();
        if (!dynCandidates.isEmpty()) {
            ModelProvider selected = weightedSelect(dynCandidates);
            log.debug("[Router] Dynamic channel selected: {} for model={}", selected.getProviderName(), model);
            return selected;
        }

        // 2. 回退到静态 Provider（application.yml 配置）
        List<ModelProvider> candidates = providers.stream()
                .filter(p -> p.supports(model))
                .toList();
        if (candidates.isEmpty()) {
            throw new BusinessException(404, "No provider available for model: " + model);
        }
        if (candidates.size() == 1) return candidates.get(0);

        List<ModelProvider> healthy = candidates.stream()
                .filter(p -> circuitBreakerService.getState(p.getProviderName()) != CircuitBreaker.State.OPEN)
                .toList();
        List<ModelProvider> pool = healthy.isEmpty() ? candidates : healthy;
        if (healthy.isEmpty()) {
            log.warn("[Router] All static providers circuit-open for model={}, fallback round-robin", model);
        }
        int idx = (roundRobinCounter.getAndIncrement() & 0x7FFFFFFF) % pool.size();
        ModelProvider selected = pool.get(idx);
        log.debug("[Router] Static provider selected: {} for model={}", selected.getProviderName(), model);
        return selected;
    }

    /**
     * 按权重加权随机选择动态渠道。
     * 权重通过 ModelProvider.getWeight() 获取（接口默认方法，动态渠道实现类返回实际配置值）。
     */
    private ModelProvider weightedSelect(List<ModelProvider> candidates) {
        int totalWeight = candidates.stream().mapToInt(ModelProvider::getWeight).sum();
        if (totalWeight <= 0) return candidates.get(0);
        int rand = (int) (Math.random() * totalWeight);
        int acc = 0;
        for (ModelProvider p : candidates) {
            acc += p.getWeight();
            if (rand < acc) return p;
        }
        return candidates.get(candidates.size() - 1);
    }

    public void recordSuccess(String providerName) {
        log.debug("[Router] recordSuccess: provider={} (handled by CircuitBreaker)", providerName);
    }

    public void recordFailure(String providerName) {
        log.debug("[Router] recordFailure: provider={} (handled by CircuitBreaker)", providerName);
    }

    public Map<String, Object> getHealthStatus() {
        Map<String, Object> status = new ConcurrentHashMap<>();
        // 静态 Provider 状态
        for (ModelProvider p : providers) {
            String name = p.getProviderName();
            CircuitBreaker.State state = circuitBreakerService.getState(name);
            status.put(name, Map.of(
                    "circuitBreakerState", state.toString(),
                    "healthy", state != CircuitBreaker.State.OPEN,
                    "type", "static"));
        }
        // 动态渠道状态
        for (ModelProvider p : dynamicProviders) {
            String name = p.getProviderName();
            CircuitBreaker.State state = circuitBreakerService.getState(name);
            status.put(name, Map.of(
                    "circuitBreakerState", state.toString(),
                    "healthy", state != CircuitBreaker.State.OPEN,
                    "type", "dynamic"));
        }
        return status;
    }
}
