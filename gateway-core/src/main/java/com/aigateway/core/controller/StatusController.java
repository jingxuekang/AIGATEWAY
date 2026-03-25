package com.aigateway.core.controller;

import com.aigateway.common.result.Result;
import com.aigateway.core.service.SmartRoutingService;
import com.aigateway.core.service.CircuitBreakerService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 网关状态接口
 * 提供各 Provider 熔断器状态，供 Dashboard 展示
 */
@Tag(name = "Gateway Status", description = "网关运行状态")
@RestController
@RequestMapping("/v1/status")
@RequiredArgsConstructor
public class StatusController {

    private final SmartRoutingService smartRoutingService;
    private final CircuitBreakerService circuitBreakerService;

    @Operation(summary = "获取各 Provider 熔断器状态")
    @GetMapping("/circuit-breakers")
    public Result<List<Map<String, Object>>> getCircuitBreakerStatus() {
        // 只返回“当前实际存在的”静态 providers + 动态 providers（来自 SmartRoutingService 的运行态路由池）
        // 避免硬编码导致展示了并未配置/未启用的 provider（例如 openai/anthropic）。
        Map<String, Object> health = smartRoutingService.getHealthStatus();

        List<Map<String, Object>> result = health.entrySet().stream().map(e -> {
            String provider = e.getKey();
            Object v = e.getValue();

            CircuitBreaker.State state;
            Boolean healthy;
            try {
                // v 形如：
                // Map.of("circuitBreakerState", state.toString(), "healthy", true/false, "type", "static/dynamic")
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) v;
                Object circuitStateObj = m.get("circuitBreakerState");
                state = circuitStateObj != null ? CircuitBreaker.State.valueOf(circuitStateObj.toString()) : circuitBreakerService.getState(provider);
                Object healthyObj = m.get("healthy");
                healthy = healthyObj instanceof Boolean ? (Boolean) healthyObj : null;
            } catch (Exception ex) {
                // 兜底：从 circuitBreakerService 再查一次
                state = circuitBreakerService.getState(provider);
                healthy = state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN;
            }

            boolean healthyFinal = healthy != null
                ? healthy
                : (state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN);

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("provider", provider);
            info.put("state", state.name());
            info.put("healthy", healthyFinal);
            info.put("label", switch (state) {
                case CLOSED -> "正常";
                case OPEN -> "熔断";
                case HALF_OPEN -> "恢复中";
                default -> state.name();
            });
            return info;
        }).toList();

        return Result.success(result);
    }

    @Operation(summary = "网关健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return Result.success(health);
    }
}
