package com.aigateway.core.service;

import com.aigateway.provider.adapter.ProviderException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CircuitBreakerService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public <T> T executeWithCircuitBreaker(String providerName, Supplier<T> supplier) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(providerName);
        try {
            return CircuitBreaker.decorateSupplier(circuitBreaker, supplier).get();
        } catch (CallNotPermittedException e) {
            log.warn("[CircuitBreaker] OPEN for provider={}, state={}",
                providerName, circuitBreaker.getState());
            throw new ProviderException(
                "Service temporarily unavailable for provider: " + providerName +
                ". Circuit breaker is OPEN, please retry after 30 seconds.",
                "CIRCUIT_BREAKER_OPEN"
            );
        } catch (Exception e) {
            log.error("[CircuitBreaker] Call failed for provider={}, error={}",
                providerName, e.getMessage());
            throw e;
        }
    }

    public Flux<String> executeStreamWithCircuitBreaker(String providerName, Supplier<Flux<String>> fluxSupplier) {
        CircuitBreaker circuitBreaker = getCircuitBreaker(providerName);

        if (!circuitBreaker.tryAcquirePermission()) {
            log.warn("[CircuitBreaker] OPEN/rejected for provider={}, rejecting stream request", providerName);
            return Flux.error(new ProviderException(
                "Service temporarily unavailable for provider: " + providerName +
                ". Circuit breaker is OPEN, please retry after 30 seconds.",
                "CIRCUIT_BREAKER_OPEN"
            ));
        }

        long startTime = System.nanoTime();
        return fluxSupplier.get()
            .doOnComplete(() -> {
                long durationNs = System.nanoTime() - startTime;
                circuitBreaker.onSuccess(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS);
                log.debug("[CircuitBreaker] Stream success for provider={}, state={}, durationMs={}",
                    providerName, circuitBreaker.getState(), durationNs / 1_000_000);
            })
            .doOnError(e -> {
                long durationNs = System.nanoTime() - startTime;
                circuitBreaker.onError(durationNs, java.util.concurrent.TimeUnit.NANOSECONDS, e);
                log.warn("[CircuitBreaker] Stream error for provider={}, state={}, error={}",
                    providerName, circuitBreaker.getState(), e.getMessage());
            });
    }

    public CircuitBreaker.State getState(String providerName) {
        return getCircuitBreaker(providerName).getState();
    }

    private CircuitBreaker getCircuitBreaker(String providerName) {
        String name = providerName != null ? providerName : "default";
        // Map dynamic channel providerName (e.g. "channel:3:deepseek") to config key (e.g. "deepseek")
        String configKey = name;
        if (name.startsWith("channel:")) {
            String[] parts = name.split(":", 3);
            if (parts.length == 3) configKey = parts[2];
        }
        return circuitBreakerRegistry.circuitBreaker(name,
            circuitBreakerRegistry.getConfiguration(configKey)
                .orElse(circuitBreakerRegistry.getDefaultConfig()));
    }
}
